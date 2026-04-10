package com.mazra3ty.app.database
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://mekxhnfmpvzrszurusmx.supabase.co",
        supabaseKey = "sb_publishable_0w0TRSkssDLzmcetsCSb8w_jQ1eW080"
    ) {
        install(Postgrest)
        install(Auth)
        install(Storage)
    }
}