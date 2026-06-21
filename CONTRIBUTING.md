# Contributing

Thanks for considering a contribution. Crumina is a small, dependency-light project,
and the goal is to keep it that way, easy to read, easy to audit, and runnable
without a toolchain.

## Before you start

- Read [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for setup and the conventions that
  matter (the client/server split, the duplicated `cleanDesc`, the bank registry, the
  CSP rules).
- For a non-trivial change, open an issue first so we can agree on the approach before
  you spend time on it.

## Ground rules

- **No build step, few dependencies.** The client stays vanilla HTML, CSS and
  JavaScript. Adding a framework, a bundler or a heavy dependency is unlikely to be
  accepted; if you think one is needed, raise it in an issue first.
- **Keep the security posture.** Do not weaken the Content-Security-Policy, do not add
  inline event handlers, and keep DOM output escaped. Do not log secrets or personal
  data.
- **Banks belong in the registry.** Add or change institutions in `app/lib/banks.js`,
  not in the parsing engines. See [docs/BANK-PARSERS.md](docs/BANK-PARSERS.md).
- **No personal data in the repo.** Never commit real statements, account numbers,
  tokens, or a populated `.env`. Use redacted fixtures.
- **Localise strings.** Add English and Bahasa Indonesia entries together.

## Making a change

1. Fork and branch from `main` with a short, descriptive branch name.
2. Make the change. Keep edits focused; a small, reviewable diff is easier to accept
   than a sweeping one.
3. Check your work:
   - `node --check` on any server file you edited.
   - Exercise affected endpoints locally and confirm the JSON.
   - For parser changes, run the module against sample text and add a fixture.
4. Write a clear commit message: what changed and why, not just what.
5. Open a pull request describing the change, how you tested it, and any follow-ups.

## Reviews

Pull requests are reviewed for correctness, security and fit with the project's
direction. Expect questions, and expect a request to split a large PR into smaller
ones. Be patient and constructive; reviewers will be the same.

## Reporting bugs and vulnerabilities

- **Bugs:** open an issue with steps to reproduce, what you expected, and what
  happened. Include the commit or version.
- **Security issues:** do not open a public issue. Follow the private disclosure steps
  in [docs/SECURITY.md](docs/SECURITY.md).

## Licensing of contributions

Crumina is licensed under the GNU Affero General Public License v3.0
([LICENSE](LICENSE)). By contributing, you agree that your contributions are licensed
under the same terms. Only submit code you have the right to contribute.
