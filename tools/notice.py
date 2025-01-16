import os

# Define the copyright notice to be added
COPYRIGHT_NOTICE = """/*
 * Copyright 2022-2025 Noah Ross
 *
 * This file is part of PerPlayerKit.
 *
 * PerPlayerKit is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * PerPlayerKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.
 */"""

def add_copyright_to_file(file_path):
    """
    Add the copyright notice to a Java file if it doesn't already exist.
    """
    with open(file_path, 'r+') as file:
        content = file.read()
        # Check if the copyright notice is already present
        if COPYRIGHT_NOTICE in content:
            print(f"Copyright notice already exists in: {file_path}")
            return
        # Prepend the copyright notice
        file.seek(0)
        file.write(COPYRIGHT_NOTICE + "\n" + content)
        print(f"Added copyright notice to: {file_path}")

def process_java_files(directory):
    """
    Recursively process all Java files in the given directory.
    """
    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith('.java'):
                file_path = os.path.join(root, file)
                add_copyright_to_file(file_path)

if __name__ == "__main__":
    # Replace this with the path to your project directory
    project_directory = "./"
    process_java_files(project_directory)

