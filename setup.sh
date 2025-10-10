cd ..
cp zenith/external_plugin_deps.bzl ./external_plugin_deps.bzl
yarn add d3 @types/d3
patch -p1 rollup.config.js zenith/patch/ignore_recursive_warning.patch
yarn install
