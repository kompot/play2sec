import com.github.kompot.play2sec.authentication.providers.oauth1.twitter
.TwitterAuthProvider
import com.github.kompot.play2sec.authentication.providers.oauth2.facebook
.FacebookAuthProvider
import com.github.kompot.play2sec.authentication.providers.oauth2.google
.GoogleAuthProvider

object SensitiveConfigurationData {
  val configByProvider: Map[String, Map[String, String]] = Map(
    FacebookAuthProvider.PROVIDER_KEY -> Map(
      "test.facebook.login" -> "",
      "test.facebook.password" -> "",
      "test.facebook.id" -> "",
      "play2sec.facebook.clientId" -> "",
      "play2sec.facebook.clientSecret" -> ""
    ),
    TwitterAuthProvider.PROVIDER_KEY -> Map(
      "test.twitter.login" -> "",
      "test.twitter.password" -> "",
      "test.twitter.id" -> "",
      "play2sec.twitter.consumerKey" -> "",
      "play2sec.twitter.consumerSecret" -> ""
    ),
    GoogleAuthProvider.PROVIDER_KEY -> Map(
      "test.google.login" -> "",
      "test.google.password" -> "",
      "test.google.id" -> "",
      "play2sec.google.clientId" -> "",
      "play2sec.google.clientSecret" -> ""
    )
  )

  // fill all the values for tests to work
  // should NOT be committed to VCS with non empty values
  val config =
    configByProvider(FacebookAuthProvider.PROVIDER_KEY) ++
    configByProvider(TwitterAuthProvider.PROVIDER_KEY) ++
    configByProvider(GoogleAuthProvider.PROVIDER_KEY)
}
