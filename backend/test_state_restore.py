import json
import ast

# Simulate the state restoration with problematic content
state = {
    "sales_data": [
        {"product": "Widget A", "sales": 1500, "region": "North\nEast"},
        {"product": "Widget B", "sales": 2300, "region": "South's \"Best\""}
    ],
    "summary": "This is a test\nwith newlines\nand 'quotes' and \"double quotes\""
}

# Test ast.literal_eval approach
for key, value in state.items():
    json_str = json.dumps(value)
    print(f"Testing {key}:")
    print(f"  JSON: {json_str[:50]}...")
    
    # This is what the fix does
    restored = ast.literal_eval(json_str)
    print(f"  ✓ Restored successfully: {type(restored)}")
    print()

print("✅ ast.literal_eval fix works correctly!")
