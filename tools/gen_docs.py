import yaml

# Input and output files
input_file = "src/main/resources/plugin.yml"
output_file = "COMMANDS.md"

# Load YAML data
with open(input_file, "r") as f:
    data = yaml.safe_load(f)

# Extract metadata
plugin_name = data.get("name", "Unknown Plugin")
version = data.get("version", "Unknown Version")
if version == "${project.version}":
    # parse the version from the pom.xml file
    with open("pom.xml", "r") as f:
        for line in f:
            if "<version>" in line:
                version = line.split(">")[1].split("<")[0]
                break
author = data.get("author", "Unknown Author")
api_version = data.get("api-version", "Unknown API Version")

# Begin writing to Markdown file
with open(output_file, "w") as f:
    # Plugin metadata
    f.write(f"# {plugin_name} Command Docs\n\n")
    f.write(f"- **Author(s):** {author}\n")
    f.write(f"- **Minimum Spigot/Paper Version:** {api_version}\n\n")

    # Commands section
    f.write("## Commands\n\n")
    f.write(
        "The following table outlines each command, its usage, aliases, and permissions required.\n\n"
    )
    f.write("| Command | Aliases | Permission |\n")
    f.write("|---------|---------|------------|\n")

    # Loop through commands and write their details
    for command, details in data.get("commands", {}).items():
        permission = details.get("permission", "N/A")

        # Handling aliases field
        aliases = details.get("aliases", "")
        if isinstance(
                aliases, list
        ):  # if aliases is a list, wrap each alias in backticks and join with commas
            aliases = ", ".join([f"`{alias}`" for alias in aliases if alias])
        elif (
                isinstance(aliases, str) and aliases
        ):  # if aliases is a single non-empty string, wrap in backticks
            aliases = f"`{aliases}`"
        else:
            aliases = (
                ""  # if aliases is None, an empty string, or another type, set to empty
            )

        f.write(f"| `{command}` | {aliases} | `{permission}` |\n")

    # Permissions section
    f.write("\n## Permissions\n\n")
    f.write(
        "The following table outlines each top-level permission and the sub-permissions it grants.\n\n"
    )
    f.write("| Permission | Grants |\n")
    f.write("|------------|--------|\n")

    def write_permission(permission, children, level=0):
        """Write permission hierarchy to the table."""
        indent = "&nbsp;" * (level * 4)  # Use HTML spaces for indentation
        grants = ", ".join(f"`{child}`" for child in children.keys())
        f.write(f"| `{permission}` | {indent}{grants} |\n")
        for child, details in children.items():
            if isinstance(details, dict) and "children" in details:
                write_permission(child, details["children"], level + 1)

    for permission, details in data.get("permissions", {}).items():
        if isinstance(details, dict) and "children" in details:
            write_permission(permission, details["children"])

print(f"Documentation generated: {output_file}")
