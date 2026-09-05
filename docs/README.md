# PerPlayerKit documentation

The source for [perplayerkit.com](https://perplayerkit.com), built with [Mintlify](https://mintlify.com).

This is the **only** home for PerPlayerKit documentation. The repository root has no
`CONFIG.md`, `COMMANDS.md`, or setup guide any more. They duplicated these pages and
drifted out of sync. The root `README.md` is a landing page that links here.

## Local preview

```bash
npm i -g mint     # or use npx mint@latest
cd docs
mint dev          # http://localhost:3000
```

Before pushing:

```bash
mint broken-links --check-anchors --check-redirects
mint validate
```

## Layout

```
docs/
├── docs.json              # site config, sidebar navigation, redirects
├── introduction.mdx       # landing page
├── downloads.mdx
├── upgrading.mdx          # config v3 migration and behavior changes
├── installation.mdx       # ─┐
├── kit-room.mdx           #  │ the setup path, in order
├── permissions-setup.mdx  # ─┘
├── features/              # one page per user-facing feature
├── commands/              # command and permission reference
├── settings/              # config.yml, storage, messages
├── data/                  # backups, export/import, migration, purge
├── help/                  # troubleshooting, FAQ
├── api.mdx                # Java API for plugin developers
├── images/                # screenshots
├── favicon.png
├── logo/                  # navbar logos (light/dark)
└── .mintlify/AGENTS.md    # instructions for the Mintlify agent
```

The sidebar groups are: **Start here**, **Features**, **Commands**, **Settings**,
**Managing data**, **Common issues**, **For developers**.

Adding a page means creating the `.mdx` file **and** listing it in `docs.json` under
`navigation.groups`. A page not listed there is unreachable.

Renaming or moving a page means adding an entry to `redirects` in `docs.json`. Old URLs
are linked from Discord, Spigot, and Modrinth, so they must keep working.

## Source of truth

Config keys, commands, and permissions must match the plugin. Verify against:

- `src/main/resources/config.yml`
- `src/main/resources/plugin.yml`
- `src/main/resources/lang/en.yml`
- `src/main/java/dev/noah/perplayerkit/`

## Screenshots

`tools/screenshots/` boots a throwaway Paper server set up for taking consistent
screenshots, and prints a shot list naming each file. See
[tools/screenshots/README.md](../tools/screenshots/README.md).

## Dashboard setup

These live in the [Mintlify dashboard](https://dashboard.mintlify.com), not in this repo:

| What                | Where                                                                      |
| ------------------- | -------------------------------------------------------------------------- |
| GitHub deployment   | Settings → GitHub. Point the deployment at this repo with `docs/` as the content directory, so every push to `main` redeploys. |
| CI checks           | Add-ons → enable **Broken links** (and optionally **Vale**) at Warning or Blocking level. Runs on PRs. |
| AI automations      | Automations → create a run triggered on repository push or a schedule. Point it at this repo so the agent opens a PR when the plugin's config or commands change. |
| Assistant           | Add-ons → Assistant. Answers reader questions using these pages as context. |

The agent reads `.mintlify/AGENTS.md` for project conventions, accuracy rules, and the
house writing style. Update that file when a new recurring mistake shows up.

`docs.json` already enables the reader-facing AI features: the contextual menu (copy page,
open in ChatGPT/Claude/Perplexity, install the MCP server in Cursor or VS Code) and search
indexing. `llms.txt` and the MCP server are generated automatically on deploy.
