load("//tools/bzl:plugin.bzl", "gerrit_plugin")

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
