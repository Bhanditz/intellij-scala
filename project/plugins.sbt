resolvers += Resolver.url("jetbrains-sbt", url(s"http://dl.bintray.com/jetbrains/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("org.jetbrains" % "sbt-ide-settings" % "1.0.0")
addSbtPlugin("org.jetbrains" % "sbt-idea-plugin" % "2.3.0-beta")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
//addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.3")