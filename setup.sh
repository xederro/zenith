cd ..
cp zenith/external_plugin_deps.bzl ./external_plugin_deps.bzl
yarn add d3 @types/d3
patch -p1 rollup.config.js zenith/patch/ignore_recursive_warning.patch
patch -p1 ../tools/bzl/junit.bzl zenith/patch/add_tech_to_prefixes.patch
yarn install
