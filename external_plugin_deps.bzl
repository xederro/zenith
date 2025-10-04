load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "handlebars",
        artifact = "com.github.jknack:handlebars:4.5.0",
        sha1 = "92a6041ba1eee8ddf79112f791f0f1bdd123a007",
    )
