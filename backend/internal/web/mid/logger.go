package mid

import (
	"bufio"
	"errors"
	"net"
	"net/http"
	"path"
	"strings"

	"github.com/go-chi/chi/v5/middleware"
	"github.com/rs/zerolog"
)

type spy struct {
	http.ResponseWriter
	status int
}

func (s *spy) WriteHeader(status int) {
	s.status = status
	s.ResponseWriter.WriteHeader(status)
}

// Write implicitly sends a 200 status if the handler hasn't called
// WriteHeader yet, matching how http.ResponseWriter behaves. Without this
// override, a handler that writes a body without an explicit WriteHeader
// call leaves s.status at its zero value.
func (s *spy) Write(b []byte) (int, error) {
	if s.status == 0 {
		s.status = http.StatusOK
	}

	return s.ResponseWriter.Write(b)
}

// Unwrap exposes the wrapped writer to http.ResponseController, which walks
// the Unwrap chain to reach the underlying connection. Without it a handler
// cannot clear the server's write deadline, and long streaming downloads get
// truncated mid-body by Web.WriteTimeout.
func (s *spy) Unwrap() http.ResponseWriter {
	return s.ResponseWriter
}

func (s *spy) Hijack() (net.Conn, *bufio.ReadWriter, error) {
	hj, ok := s.ResponseWriter.(http.Hijacker)
	if !ok {
		return nil, nil, errors.New("response writer does not support hijacking")
	}
	return hj.Hijack()
}

// isQuietPath checks if a request is a static asset or high-frequency healthcheck
// that should not spam production console logs at INFO level when successful.
func isQuietPath(p string) bool {
	if strings.HasPrefix(p, "/_nuxt/") ||
		strings.HasPrefix(p, "/assets/") ||
		p == "/api/v1/status" ||
		p == "/status" ||
		p == "/health" ||
		p == "/healthz" ||
		p == "/favicon.ico" ||
		p == "/robots.txt" ||
		p == "/manifest.json" ||
		p == "/sw.js" {
		return true
	}

	ext := path.Ext(p)
	switch ext {
	case ".js", ".css", ".map", ".svg", ".png", ".jpg", ".jpeg", ".webp", ".woff", ".woff2", ".ttf", ".ico":
		return true
	}

	return false
}

func Logger(l zerolog.Logger) func(http.Handler) http.Handler {
	return func(h http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			reqID, _ := r.Context().Value(middleware.RequestIDKey).(string)
			quiet := isQuietPath(r.URL.Path)

			// Log "request received" at Debug level to avoid duplicate logging per request in Info mode
			l.Debug().Ctx(r.Context()).Str("method", r.Method).Str("path", r.URL.Path).Str("rid", reqID).Msg("request received")

			s := &spy{ResponseWriter: w}
			h.ServeHTTP(s, r)

			// For static assets and frequent health check probes, only log at Info/Warn/Error if something failed (>= 400).
			// Successful 2xx/3xx requests are downgraded to Debug level.
			if quiet {
				if s.status >= 500 {
					l.Error().Ctx(r.Context()).Str("method", r.Method).Str("path", r.URL.Path).Int("status", s.status).Str("rid", reqID).Msg("request finished")
				} else if s.status >= 400 {
					l.Warn().Ctx(r.Context()).Str("method", r.Method).Str("path", r.URL.Path).Int("status", s.status).Str("rid", reqID).Msg("request finished")
				} else {
					l.Debug().Ctx(r.Context()).Str("method", r.Method).Str("path", r.URL.Path).Int("status", s.status).Str("rid", reqID).Msg("request finished")
				}
				return
			}

			// Regular API and app requests
			if s.status >= 500 {
				l.Error().Ctx(r.Context()).Str("method", r.Method).Str("path", r.URL.Path).Int("status", s.status).Str("rid", reqID).Msg("request finished")
			} else if s.status >= 400 {
				l.Warn().Ctx(r.Context()).Str("method", r.Method).Str("path", r.URL.Path).Int("status", s.status).Str("rid", reqID).Msg("request finished")
			} else {
				l.Info().Ctx(r.Context()).Str("method", r.Method).Str("path", r.URL.Path).Int("status", s.status).Str("rid", reqID).Msg("request finished")
			}
		})
	}
}
