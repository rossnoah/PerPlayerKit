import yaml
from tabulate import tabulate

# Input and output files
input_file = "src/main/resources/plugin.yml"
output_file = "COMMANDS.md"

# Load YAML data
try:
    with open(input_file, "r") as f:
        data = yaml.safe_load(f)
except FileNotFoundError:
    raise FileNotFoundError(f"Could not find the file: {input_file}")

# Extract metadata with defaults
plugin_name = data.get("name", "Unknown Plugin")
version = data.get("version", "Unknown Version")

# If the version contains a placeholder, attempt to parse it from `pom.xml`
if version == "${project.version}":
    try:
        with open("pom.xml", "r") as f:
            for line in f:
                if "<version>" in line:
                    version = line.split(">")[1].split("<")[0]
                    break
    except FileNotFoundError:
        version = "Unknown Version (pom.xml missing)"

author = data.get("author", "Unknown Author")
api_version = data.get("api-version", "Unknown API Version")

# Prepare commands for the table
commands_table = []
for command, details in data.get("commands", {}).items():
    aliases = details.get("aliases", "N/A")
    if isinstance(aliases, list):
        aliases = ", ".join(aliases)
    permission = details.get("permission", "N/A")
    commands_table.append([f"`{command}`", f"`{aliases}`", f"`{permission}`"])

# Prepare permissions for the table
permissions_table = []
for permission, details in data.get("permissions", {}).items():
    if isinstance(details, dict):
        children = details.get("children", {})
        grants = ", ".join(f"`{child}`" for child in children.keys()) if children else "N/A"
        permissions_table.append([f"`{permission}`", grants])

# Begin writing to the Markdown file
with open(output_file, "w") as f:
    # Plugin metadata
    f.write(f"# {plugin_name} Command Docs\n\n")
    f.write(f"- **Version:** {version}\n")
    f.write(f"- **Author(s):** {author}\n")
    f.write(f"- **Minimum Spigot/Paper Version:** {api_version}\n\n")

    # Commands section
    f.write("## Commands\n\n")
    f.write("The following table outlines each command, its usage, aliases, and permissions required.\n\n")
    f.write(tabulate(commands_table, headers=["Command", "Aliases", "Permission"], tablefmt="github"))
    f.write("\n\n")

    # Permissions section
    f.write("## Permissions\n\n")
    f.write("The following table outlines each top-level permission and the sub-permissions it grants.\n\n")
    f.write(tabulate(permissions_table, headers=["Permission", "Grants"], tablefmt="github"))

print(f"Documentation generated: {output_file}")
