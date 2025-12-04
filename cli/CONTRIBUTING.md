# Contributing

Contributions to this project
are [released](https://help.github.com/articles/github-terms-of-service/#6-contributions-under-repository-license) to
the public under the [project's open source license](LICENSE.md).

Everyone is welcome to contribute to SPHA by submitting pull requests, suggesting new features or
reporting [issues](https://github.com/fraunhofer-iem/software-product-health-analyzer/issues). Please, also read through
the [Contributor Covenant Code of Conduct](http://contributor-covenant.org). By participating, you agree to uphold this
code.

## Pull Requests

We are happy to accept your pull request. Here's a quick guide:

1. For non-trivial changes, reuse an existing issue or create a new one.
2. Fork the repo, and clone it locally and create a new branch (`git checkout -b mybranch`).
    * Name the branch so that it clearly communicates your intentions.
    * You may use prefixes such as `feature/myfeature` or `bugfix/myfix`.
3. Make and commit your changes to your branch. Please follow our [Commit Message guideline](#commit-messages).
4. If applicable, add new tests corresponding to your changes. Ensure all tests are passing.
5. Create your pull request (PR) against the `main` branch.

At this point you're waiting on us. We like to at least comment on, if not
accept, pull requests within a few days. We may suggest some changes or improvements or alternatives.

Some things that will increase the chance that your pull request is accepted:
* Follow the [Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html).
* Make sure the tests pass.
* Update the documentation: code comments, example code, guides. Basically, update everything affected by your
  contribution.
* Include any information that would be relevant to reproducing bugs, use cases for new features, etc.
* Associate your commits with your [GitHub
  user](https://help.github.com/articles/why-are-my-commits-linked-to-the-wrong-user/).

# Commit Messages

Commit shall follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/#specification) rules:

* Commit test files with `test: …` prefix.
* Commit bug fixes with `fix: …` or prefix
* Commit features with `feat: …` or `prefix
* Commit breaking changes by adding `BREAKING CHANGE:` in the commit body.
  The commit subject does not matter. A commit can have multiple `BREAKING CHANGE:` sections
* Commit changes to `.gitignore` and other meta files with `chore: …`
* Commit changes to README files or comments with `docs: …`

* Also, use imperative, present tense: `change` not `changed` nor `changes`

Example commit:

```
fix: Correct KPI calculation for aggregate strategy

Change aggretate strategy from multiplication to addition. 

Fix #42
```

## Stale issue and pull request policy

Issues and pull requests have a shelf life and sometimes they are no longer relevant. All issues and pull requests that
have not had any activity for 90 days will be marked as `stale`. Simply leave a comment with information about why it
may still be relevant to keep it open. If no activity occurs in the next 7 days, it will be automatically closed.

The goal of this process is to keep the list of open issues and pull requests focused on work that is actionable and
important for the maintainers and the community.

## File Headers

Please use the following file header for new source code files.

```kotlin
/*
* Copyright(c)[YEAR] Fraunhofer IEM. All rights reserved.
* 
* Licensed under the MIT license. See LICENSE file in the project root for details.
*
* SPDX-License-Identifier: MIT
* License-Filename: LICENSE
/*
```
