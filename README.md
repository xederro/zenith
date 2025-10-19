# Zenith Gerrit Plugin
## DEVELOPMENT

Install dependencies:

- Node.js (v20 or higher)
- npm (v11 or higher)
- yarn (v1.22 or higher)
- Java (21 or higher)
- bazelisk (v7 or higher)

Clone Gerrit repository and build with Java 21:

```bash
git clone --recurse-submodules https://gerrit.googlesource.com/gerrit
cd gerrit && bazel build --config=java21
```

Clone Zenith repository in the plugins directory:

```bash
git clone https://github.com/xederro/zenith.git
```

Run setup.sh to install dependencies:

```bash
./setup.sh
```

Build the plugin:

```bash
bazel build --config=java21 plugins/zenith
```
