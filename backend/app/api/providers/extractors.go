package providers

import (
	"errors"
	"mime"
	"net/http"
	"strings"

	"github.com/hay-kot/httpkit/server"
	"github.com/rs/zerolog/log"
	"github.com/sysadminsmedia/homebox/backend/internal/sys/validate"
)

type LoginForm struct {
	Username     string `json:"username"`
	Email        string `json:"email"`
	Password     string `json:"password"`
	StayLoggedIn bool   `json:"stayLoggedIn"`
}

func getLoginForm(r *http.Request) (LoginForm, error) {
	loginForm := LoginForm{}

	ct := r.Header.Get("Content-Type")
	mediaType, _, _ := mime.ParseMediaType(ct)
	if mediaType == "" {
		mediaType = strings.TrimSpace(strings.Split(ct, ";")[0])
	}

	switch mediaType {
	case "application/x-www-form-urlencoded":
		err := r.ParseForm()
		if err != nil {
			return loginForm, errors.New("failed to parse form")
		}

		loginForm.Username = r.PostFormValue("username")
		if loginForm.Username == "" {
			loginForm.Username = r.PostFormValue("email")
		}
		loginForm.Password = r.PostFormValue("password")
		loginForm.StayLoggedIn = r.PostFormValue("stayLoggedIn") == "true"
	case "application/json":
		err := server.Decode(r, &loginForm)
		if err != nil {
			log.Err(err).Msg("failed to decode login form")
			return loginForm, errors.New("failed to decode login form")
		}
	default:
		return loginForm, errors.New("invalid content type")
	}

	if loginForm.Username == "" && loginForm.Email != "" {
		loginForm.Username = loginForm.Email
	}

	if loginForm.Username == "" || loginForm.Password == "" {
		return loginForm, validate.NewFieldErrors(
			validate.FieldError{
				Field: "username",
				Error: "username or password is empty",
			},
			validate.FieldError{
				Field: "password",
				Error: "username or password is empty",
			},
		)
	}

	return loginForm, nil
}
