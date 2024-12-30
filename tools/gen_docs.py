#run this from the root of the project to generate the command documentation

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
    f.write(f"# {plugin_name} Command Docs\n\n")
    f.write(f"- **Author(s):** {author}\n")
    f.write(f"- **Minimum Spigot/Paper Version:** {api_version}\n\n")
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

print(f"Documentation generated: {output_file}")
