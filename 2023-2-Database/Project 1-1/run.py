import lark.exceptions
from lark import Lark, Transformer
from berkeleydb import db


# Transformer.transform() runs the functions have same name with token
class MyTransformer(Transformer):
    def create_table_query(self, items):
        print("'CREATE TABLE' requested")

    def drop_table_query(self, items):
        print("'DROP TABLE' requested")

    def explain_query(self, items):
        print("'EXPLAIN' requested")

    def describe_query(self, items):
        print("'DESCRIBE' requested")

    def desc_query(self, items):
        print("'DESC' requested")

    def insert_query(self, items):
        print("'INSERT' requested")

    def delete_query(self, items):
        print("'DELETE' requested")

    def select_query(self, items):
        print("'SELECT' requested")

    def show_tables_query(self, items):
        print("'SHOW TABLES' requested")

    def update_query(self, items):
        print("'UPDATE' requested")

    def EXIT(self, items):
        exit()


with open('grammar.lark') as file:
    sql_parser = Lark(file.read(), start="command", lexer="basic")

while True:
    try:
        raw_input = input("DB_2020-12852> ")
        while not raw_input.endswith(';'):          # input always ends with semicolon
            raw_input += '\n' + input()

        commands = raw_input.split(';')[:-1]        # last element is empty string
        for command in commands:
            command += ';'                          # attaching semicolon removed by split
            output = sql_parser.parse(command)      # lark parses output
            MyTransformer().transform(output)       # transform() runs the function that correspond to statement
    # lark raises exceptions inheriting LarkError, so catch LarkError for every exception
    except lark.exceptions.LarkError:
        print("Syntax error")
