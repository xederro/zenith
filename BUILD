load("//tools/bzl:plugin.bzl", "gerrit_plugin")
load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)

gerrit_plugin(
    name = "zenith",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: zenith",
        "Gerrit-Module: tech.xederro.zenith.ZenithModule",
        "Gerrit-SshModule: tech.xederro.zenith.ZenithSshModule",
        "Gerrit-HttpModule: tech.xederro.zenith.ZenithHttpModule",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        "@handlebars//jar",
    ],
    resource_jars = [
        "//plugins/zenith/frontend:zenith",
    ],
)

junit_tests(
    name = "zenith_tests",
    testonly = 1,
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["zenith"],
    deps = [
        ":zenith__plugin_test_deps",
    ],
)

java_library(
    name = "zenith__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":zenith__plugin",
        "@commons-lang3//jar",
        "@handlebars//jar",
    ],
)
