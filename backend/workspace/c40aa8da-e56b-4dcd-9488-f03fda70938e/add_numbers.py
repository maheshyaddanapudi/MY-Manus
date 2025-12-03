def add_numbers(a, b):
        """
        Simple function to add two numbers
        
        Args:
            a: First number
            b: Second number
        
        Returns:
            Sum of a and b
        """
        return a + b
    
    # Example usage
    if __name__ == "__main__":
        # Test the function with some examples
        result1 = add_numbers(5, 3)
        result2 = add_numbers(10.5, 2.7)
        result3 = add_numbers(-4, 9)
        
        print("Testing add_numbers function:")
        print(f"add_numbers(5, 3) = {result1}")
        print(f"add_numbers(10.5, 2.7) = {result2}")
        print(f"add_numbers(-4, 9) = {result3}")
    