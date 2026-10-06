#!/usr/bin/env python3
import os
import sys
import xml.etree.ElementTree as ET

def find_string_resources(root_dir):
    results = []
    for dirpath, dirnames, filenames in os.walk(root_dir):
        # Skip build, .gradle, .git, .idea, scratch
        if any(skip in dirpath for skip in ["build", ".gradle", ".git", ".idea", "scratch"]):
            continue
        if os.path.basename(dirpath) == "values" and os.path.basename(os.path.dirname(dirpath)) == "res":
            en_file = os.path.join(dirpath, "strings.xml")
            ru_dir = os.path.join(os.path.dirname(dirpath), "values-ru")
            ru_file = os.path.join(ru_dir, "strings.xml")
            if os.path.exists(en_file):
                results.append((en_file, ru_file))
    return results

def get_string_keys(file_path):
    if not os.path.exists(file_path):
        return set()
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
        keys = set()
        for child in root:
            if child.tag in ["string", "string-array", "plurals"]:
                name = child.attrib.get("name")
                if name:
                    keys.add(name)
        return keys
    except Exception as e:
        print(f"Error parsing XML file {file_path}: {e}")
        return set()

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(script_dir, ".."))

    print("=== Localization Audit Tool ===")
    print(f"Scanning project at: {project_root}\n")

    pairs = find_string_resources(project_root)
    if not pairs:
        print("No strings.xml files found.")
        sys.exit(0)

    has_errors = False
    for en_path, ru_path in pairs:
        rel_en = os.path.relpath(en_path, project_root)
        rel_ru = os.path.relpath(ru_path, project_root)
        print(f"Module resource pair:\n  EN: {rel_en}\n  RU: {rel_ru}")

        en_keys = get_string_keys(en_path)
        ru_keys = get_string_keys(ru_path)

        missing_in_ru = en_keys - ru_keys
        missing_in_en = ru_keys - en_keys

        if not ru_keys and missing_in_ru:
            print("  [ERROR] Corresponding RU values-ru/strings.xml missing or empty!")
            has_errors = True
        elif missing_in_ru or missing_in_en:
            has_errors = True
            if missing_in_ru:
                print("  [ERROR] Missing keys in RU (values-ru/strings.xml):")
                for k in sorted(missing_in_ru):
                    print(f"    - {k}")
            if missing_in_en:
                print("  [ERROR] Missing keys in EN (values/strings.xml):")
                for k in sorted(missing_in_en):
                    print(f"    - {k}")
        else:
            print(f"  [OK] 100% key parity achieved ({len(en_keys)} keys match).")
        print()

    if has_errors:
        print("RESULT: Localization audit FAILED due to parity discrepancies.")
        sys.exit(1)
    else:
        print("RESULT: Localization audit PASSED successfully!")
        sys.exit(0)

if __name__ == "__main__":
    main()
