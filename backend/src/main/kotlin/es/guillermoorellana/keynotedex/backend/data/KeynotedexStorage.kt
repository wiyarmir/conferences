package es.guillermoorellana.keynotedex.backend.data

import es.guillermoorellana.keynotedex.backend.data.conferences.ConferenceStorage
import es.guillermoorellana.keynotedex.backend.data.sessions.SessionStorage
import es.guillermoorellana.keynotedex.backend.data.users.UserStorage
import java.io.Closeable

interface KeynotedexStorage :
    Closeable,
    UserStorage,
    ConferenceStorage,
    SessionStorage
