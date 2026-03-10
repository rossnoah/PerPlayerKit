import os
import re
import subprocess
from datetime import datetime


NOTICE_TEMPLATE = """/*
 * Copyright {years} Noah Ross
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

COPYRIGHT_LINE_RE = re.compile(
    r"^ \* Copyright (?P<years>\d{4}(?:-\d{4})?) Noah Ross$",
    re.MULTILINE,
)


def get_repo_root(start_path):
    try:
        result = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            cwd=start_path,
            capture_output=True,
            text=True,
            check=True,
        )
    except (FileNotFoundError, subprocess.CalledProcessError):
        return None

    return result.stdout.strip()


def get_copyright_years(file_path, repo_root):
    if repo_root is None:
        return str(datetime.now().year)

    relative_path = os.path.relpath(file_path, repo_root)

    try:
        result = subprocess.run(
            [
                "git",
                "-C",
                repo_root,
                "log",
                "--follow",
                "--format=%ad",
                "--date=format:%Y",
                "--",
                relative_path,
            ],
            capture_output=True,
            text=True,
            check=True,
        )
    except (FileNotFoundError, subprocess.CalledProcessError, ValueError):
        return str(datetime.now().year)

    years = [line.strip() for line in result.stdout.splitlines() if line.strip()]
    if not years:
        return str(datetime.now().year)

    newest_year = years[0]
    oldest_year = years[-1]
    if newest_year == oldest_year:
        return newest_year

    return f"{oldest_year}-{newest_year}"


def write_updated_content(file_path, content):
    with open(file_path, "w", encoding="utf-8") as file:
        file.write(content)


def add_copyright_to_file(file_path, repo_root):
    """
    Add or update the copyright notice on a Java file.
    """
    with open(file_path, "r", encoding="utf-8") as file:
        content = file.read()

    years = get_copyright_years(file_path, repo_root)
    notice = NOTICE_TEMPLATE.format(years=years)

    match = COPYRIGHT_LINE_RE.search(content)
    if match:
        if match.group("years") == years:
            print(f"Copyright notice already up to date in: {file_path}")
            return

        updated_content = COPYRIGHT_LINE_RE.sub(
            f" * Copyright {years} Noah Ross",
            content,
            count=1,
        )
        write_updated_content(file_path, updated_content)
        print(f"Updated copyright notice in: {file_path}")
        return

    write_updated_content(file_path, notice + "\n" + content)
    print(f"Added copyright notice to: {file_path}")


def process_java_files(directory, repo_root):
    """
    Recursively process all Java files in the given directory.
    """
    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith(".java"):
                file_path = os.path.join(root, file)
                add_copyright_to_file(file_path, repo_root)


if __name__ == "__main__":
    project_directory = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    repo_root = get_repo_root(project_directory)
    process_java_files(project_directory, repo_root)
