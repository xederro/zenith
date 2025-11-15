***

# @PLUGIN@ apply

**SSH command for applying templates to repositories.**

***

## SYNOPSIS

```
ssh -p <port> <host> zenith apply
  [--template-targets <TEMPLATE-TARGETS> | -tt <TEMPLATE-TARGETS>]
  [--json <JSON> | -j <JSON>]
  [--override]
  { <NAME> }
```

***

## DESCRIPTION

The `zenith apply` command allows for application of predefined templates to existing projects on Gerrit.
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
$ ssh -p 29418 review.example.com zenith apply templates/base \
  --template-targets "template@refs/heads/main:refs/heads/dev" \
  --json '{"projectOwner": "dev-team", "branch": "dev"}'
```
---
