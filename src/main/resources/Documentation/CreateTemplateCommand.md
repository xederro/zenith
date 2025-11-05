***

# Zenith create

**SSH command for project creation from templates.**

***

## SYNOPSIS

```
ssh -p <port> <host> zenith create
  [--owner <GROUP> ... | -o <GROUP> ...]
  [--parent <NAME> | -p <NAME> ]
  [--suggest-parents | -S ]
  [--permissions-only]
  [--description <DESC> | -d <DESC>]
  [--submit-type <TYPE> | -t <TYPE>]
  [--use-contributor-agreements | --ca]
  [--use-signed-off-by | --so]
  [--use-content-merge]
  [--create-new-change-for-all-not-in-target]
  [--require-change-id | --id]
  [[--branch <REF> | -b <REF>] ...]
  [--empty-commit]
  [--max-object-size-limit <N>]
  [--plugin-config <PARAM> ...]
  [--template-targets <TEMPLATE-TARGETS> | -tt <TEMPLATE-TARGETS>]
  [--json <JSON> | -j <JSON>]
  [--override]
  { <NAME> }
```

***

## DESCRIPTION

The `zenith create` command allows for the creation of new projects in Gerrit using predefined templates.
It supports specifying multiple template target mappings and injecting JSON data for dynamic content generation.

***

## SCRIPTING

This command is intended to be used in scripts.

***

## OPTIONS

### `--template-targets` / `-tt`
Specifies one or more template target mappings in the format:
```
from@ref:to,from@ref:to
```
Multiple targets can be separated by commas.  
Each mapping defines how content should be templated from one source reference to a target reference.

Example:
```
--template-targets "template@refs/heads/main:refs/heads/dev,template@refs/heads/main:refs/heads/test"
```

### `--json` / `-j`
Provides a JSON string containing input values for templates.  
Used to inject variables or configuration data when processing project templates.

Example:
```
--json '{"branch": "main", "team": "qa"}'
```

### `--override`
If present the change will remove everything that is not in template.

***

## EXAMPLES

**Create a project with template targets and JSON data:**
```
$ ssh -p 29418 review.example.com zenith create templates/base \
  --template-targets "template@refs/heads/main:refs/heads/dev" \
  --json '{"projectOwner": "dev-team", "branch": "dev"}'
```
---
