package es.guillermoorellana.keynotedex.web.screens

import es.guillermoorellana.keynotedex.web.comms.*
import es.guillermoorellana.keynotedex.web.external.*
import es.guillermoorellana.keynotedex.web.model.*
import kotlinx.coroutines.*
import kotlinx.html.*
import kotlinx.html.js.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import react.*
import react.dom.*

//language=CSS
private const val css = """
.form-signin {
  max-width: 330px;
  padding: 15px;
  margin: 0 auto;
}
.form-signin .form-signin-heading {
  margin-bottom: 10px;
  margin-top: 10px;
}
.form-signin .form-control {
  position: relative;
  box-sizing: border-box;
  height: auto;
  padding: 10px;
  font-size: 16px;
}
.form-signin .form-control:focus {
  z-index: 2;
}
.form-signin input[type="email"] {
  margin-bottom: -1px;
  border-bottom-right-radius: 0;
  border-bottom-left-radius: 0;
}
.form-signin input[type="password"] {
  margin-bottom: 10px;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
}
"""

class LoginScreen : RComponent<LoginProps, LoginState>() {

    override fun LoginState.init() {
        disabled = false
        login = ""
        password = ""
        errorMessage = null
    }

    override fun RBuilder.render() {
        style { +css }
        if (isUserLoggedIn()) {
            redirect("/${props.currentUser!!.userId}") {}
            return
        }
        form(classes = "form-signin") {
            attrs {
                onSubmitFunction = {
                    it.preventDefault()
                    doLogin()
                }
            }
            h2("form-signin-heading") { +"Welcome back!" }
            label(classes = "sr-only") {
                attrs { htmlFor = "inputEmail" }
                +"Email address"
            }
            input(type = InputType.text, classes = "form-control") {
                attrs {
                    id = "inputEmail"
                    placeholder = "Email"
                    required = true
                    autoFocus = true
                    value = state.login
                    disabled = state.disabled
                    onChangeFunction = { event ->
                        val value = event.inputValue
                        setState {
                            login = value
                        }
                    }
                }
            }
            label("sr-only") {
                attrs { htmlFor = "inputPassword" }
            }
            input(type = InputType.password, classes = "form-control") {
                attrs {
                    id = "inputPassword"
                    placeholder = "Password"
                    required = true
                    value = state.password
                    disabled = state.disabled
                    onChangeFunction = { event ->
                        val value = event.inputValue
                        setState {
                            password = value
                        }
                    }
                }
            }
            button(classes = "btn btn-lg btn-primary btn-block", type = ButtonType.submit) { +"Sign in" }
            routeLink("/register") {
                attrs {
                    className = "btn btn-lg btn-secondary btn-block"
                }
                +"Register"
            }
//            a(href = "/login/github", classes = "btn btn-lg btn-dark btn-block") {
//                +"Login via GitHub"
//            }
        }
    }

    private fun doLogin() {
        setState {
            disabled = true
        }
        GlobalScope.promise {
            val user = login(state.login, state.password)
            loggedIn(user)
        }.catch { err -> loginFailed(err) }
    }

    private fun loggedIn(user: User) {
        props.onUserLoggedIn(user)
    }

    private fun loginFailed(err: Throwable) {
        if (err is LoginOrRegisterFailedException) {
            setState {
                disabled = false
                errorMessage = err.message
            }
        } else {
            console.error("Login failed", err)
            setState {
                disabled = false
                errorMessage = "Login failed: please reload page and try again"
            }
        }
    }

    private fun isUserLoggedIn() = props.currentUser != null
}

external interface LoginProps : RProps {
    var currentUser: User?
    var onUserLoggedIn: (User) -> Unit
}

external interface LoginState : RState {
    var login: String
    var password: String
    var disabled: Boolean
    var errorMessage: String?
}

internal val Event.inputValue: String
    get() = (target as? HTMLInputElement)?.value ?: (target as? HTMLTextAreaElement)?.value ?: ""

fun RBuilder.login(handler: RHandler<LoginProps>) = child(LoginScreen::class, handler)
