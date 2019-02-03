package es.guillermoorellana.keynotedex.web.screens

import es.guillermoorellana.keynotedex.web.comms.NetworkDataSource
import es.guillermoorellana.keynotedex.web.comms.WithNetworkDataSource
import es.guillermoorellana.keynotedex.web.context.UserContext
import es.guillermoorellana.keynotedex.web.external.redirect
import es.guillermoorellana.keynotedex.web.external.routeLink
import es.guillermoorellana.keynotedex.web.model.User
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import react.RBuilder
import react.RComponent
import react.RHandler
import react.RState
import react.dom.button
import react.dom.div
import react.dom.form
import react.dom.h2
import react.dom.input
import react.dom.label
import react.dom.style
import react.setState

// language=CSS
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

class SignInScreen : RComponent<SignInProps, SignInState>() {

    override fun SignInState.init() {
        disabled = false
        login = ""
        password = ""
        errorMessage = null
    }

    override fun RBuilder.render() {
        style { +css }
        form(classes = "form-signin") {
            attrs {
                onSubmitFunction = { event ->
                    event.preventDefault()
                    doLogin()
                }
            }
            h2("form-signin-heading") { +"Welcome back!" }
            state.errorMessage?.let { message ->
                div("alert alert-danger") { +message }
            }
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
            button(classes = "btn btn-lg btn-primary btn-block", type = ButtonType.submit) { +"Sign In" }
            routeLink("/signup") {
                attrs {
                    className = "btn btn-lg btn-secondary btn-block"
                }
                +"Sign Up"
            }
//            a(href = "/signin/github", classes = "btn btn-lg btn-dark btn-block") {
//                +"Login via GitHub"
//            }
            UserContext.Consumer { user ->
                user?.let {
                    redirect("/${it.userId}") {}
                }
            }
        }
    }

    private fun doLogin() {
        setState {
            disabled = true
        }
        GlobalScope.launch {
            val user = try {
                props.networkDataSource.login(state.login, state.password)
            } catch (err: NetworkDataSource.LoginOrRegisterFailedException) {
                setState {
                    errorMessage = err.message
                }
                null
            } finally {
                setState {
                    disabled = false
                }
            }
            user?.let { props.onUserLoggedIn(it) }
        }
    }
}

external interface SignInProps : WithNetworkDataSource {
    var onUserLoggedIn: (User) -> Unit
}

external interface SignInState : RState {
    var login: String
    var password: String
    var disabled: Boolean
    var errorMessage: String?
}

internal val Event.inputValue: String
    get() = (target as? HTMLInputElement)?.value ?: (target as? HTMLTextAreaElement)?.value ?: ""

fun RBuilder.signIn(handler: RHandler<SignInProps>) = child(SignInScreen::class, handler)
