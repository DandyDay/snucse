import lark.exceptions
from lark import Lark, Transformer
from berkeleydb import db
import os
import json
import re


class Prompt:

    def get_input(self):
        raw_input = input("DB_2020-12852> ")
        return raw_input

    def show_prompt(self):
        print("DB_2020-12852> ", end="")

    def syntax_error(self):
        print("Syntax error")

    def create_table_success(self, table_name):
        Prompt().show_prompt()
        print("'%s' table is created" % table_name)

    def duplicate_column_def_error(self):
        Prompt().show_prompt()
        print("Create table has failed: column definition is duplicated")

    def duplicate_primary_key_def_error(self):
        Prompt().show_prompt()
        print("Create table has failed: primary key definition is duplicated")

    def reference_type_error(self):
        Prompt().show_prompt()
        print("Create table has failed: foreign key references wrong type")

    def reference_non_primary_key_error(self):
        Prompt().show_prompt()
        print("Create table has failed: foreign key references non primary key column")

    def reference_column_existence_error(self):
        Prompt().show_prompt()
        print("Create table has failed: foreign key references non existing column")

    def reference_table_existence_error(self):
        Prompt().show_prompt()
        print("Create table has failed: foreign key references non existing table")

    def non_existing_column_def_error(self, col_name):
        Prompt().show_prompt()
        print("Create table has failed: '%s' does not exist in column definition" % col_name)

    def table_existence_error(self):
        Prompt().show_prompt()
        print("Create table has failed: table with the same name already exists")

    def char_length_error(self):
        Prompt().show_prompt()
        print("Char length should be over 0")

    def drop_success(self, table_name):
        Prompt().show_prompt()
        print("'%s' table is dropped" % table_name)

    def no_such_table(self):
        Prompt().show_prompt()
        print("No such table")

    def drop_referenced_table_error(self, table_name):
        Prompt().show_prompt()
        print("Drop" + " table has failed: '%s' is referenced by other table" % table_name)

    def insert_result(self):
        Prompt().show_prompt()
        print("The row is inserted")

    def select_table_existence_error(self, table_name):
        Prompt().show_prompt()
        print("Selection has failed: '%s' does not exist" % table_name)


'''
check if column name exists in table schema 
'''


def check_column_exist(column_name, table_schema):
    for column_def in table_schema["columns"]:
        if column_def["name"].lower() == column_name.lower():
            return True
    return False


'''
get type of the column in table schema
'''


def get_column_type(column_name, table_schema):
    for column_def in table_schema["columns"]:
        if column_def["name"].lower() == column_name.lower():
            return column_def["type"]
    return None


'''
returns N when type is char(N)
'''


def get_char_type_len(type):
    match = re.search(r'\d+', type)
    if match:
        return int(match.group())
    else:
        return -1


'''
get table schema from db file
'''


def get_table_schema(myDB, table_name):
    db_cursor = myDB.cursor()
    table_schema = None
    while x := db_cursor.next():
        temp_schema = json.loads(x[1])
        if table_name == temp_schema["table_name"]:
            table_schema = temp_schema
            break
    return table_schema


'''
check if the column is foreign key in table_schema
'''


def is_foreign_key(column_name, table_schema):
    fk_constraint = table_schema["foreign_key"]
    for fk in fk_constraint:
        if column_name in fk['foreign_keys']:
            return True
    return False


# Transformer.transform() runs the functions have same name with token
class MyTransformer(Transformer):

    def __init__(self, prompt, myDB):
        super().__init__()
        self.prompt = prompt
        self.myDB = myDB

    def create_table_query(self, items):
        table_name = items[2].children[0].lower()

        # Check table existence
        if myDB.get(table_name.encode('utf-8')) is not None:
            prompt.table_existence_error()
            return

        table_schema = {"table_name": table_name, 'columns': [], "primary_key": [], "foreign_key": []}
        column_definition_iter = items[3].find_data("column_definition")
        for column_definition in column_definition_iter:
            # Check duplicate column
            for column_name in table_schema['columns']:
                if column_name["name"] == column_definition.children[0].children[0].lower():
                    prompt.duplicate_column_def_error()
                    return

            # Check type
            type = "".join(column_definition.children[1].children)
            if type.startswith("char") and get_char_type_len(type) < 1:
                prompt.char_length_error()
                return

            # Check nullable
            nullable = True
            if column_definition.children[2] is not None and column_definition.children[
                2].lower() == "not":  # not으로 시작하는데 not null이 아니면 파싱에서 걸러짐
                nullable = False
            column_info = {"name": column_definition.children[0].children[0].lower(), "type": type,
                           "nullable": nullable}
            table_schema['columns'].append(column_info)

        # Check primary key constraint
        primary_key_constraint_iter = items[3].find_data("primary_key_constraint")
        primary_key_count = 0
        for pk_constraint in primary_key_constraint_iter:
            # Check more than one pk_constraint
            if primary_key_count > 0:
                prompt.duplicate_primary_key_def_error()
                return

            primary_key_columns = pk_constraint.children[2].children
            for pk_column in primary_key_columns[1:-1]:
                column_name = pk_column.children[0].lower()

                # Check column definition exists
                if not check_column_exist(column_name, table_schema):
                    prompt.non_existing_column_def_error(column_name)
                    return
                # Check duplicate primary key
                if column_name in table_schema['primary_key']:
                    prompt.duplicate_primary_key_def_error()
                    return
                # Append primary key to schema
                else:
                    for column in table_schema['columns']:
                        if column["name"] == column_name:
                            column["nullable"] = False
                    table_schema['primary_key'].append(column_name)
            primary_key_count += 1

        # Check foreign key constraints
        foreign_key_constraint_iter = items[3].find_data("referential_constraint")
        for fk_constraint in foreign_key_constraint_iter:
            reference_table = fk_constraint.children[4].children[0]
            foreign_key_schema = {"reference_table": reference_table, "foreign_keys": [], "reference_keys": []}

            reference_table_schema = myDB.get(reference_table.encode('utf-8'))
            # Check if referenced table exists
            if reference_table_schema is None:
                prompt.reference_table_existence_error()
                return

            reference_table_schema = json.loads(reference_table_schema)

            foreign_keys_iter = fk_constraint.children[2].children
            reference_keys_iter = fk_constraint.children[5].children

            for foreign_key in foreign_keys_iter[1:-1]:
                foreign_key_schema["foreign_keys"].append(foreign_key.children[0].value)
            for reference_key in reference_keys_iter[1:-1]:
                foreign_key_schema["reference_keys"].append(reference_key.children[0].value)

            reference_table_pk = reference_table_schema['primary_key']

            for i in range(len(foreign_key_schema["foreign_keys"])):
                foreign_key = foreign_key_schema["foreign_keys"][i]
                # Check foreign key is not in column definitions
                if not check_column_exist(foreign_key, table_schema):
                    prompt.non_existing_column_def_error(foreign_key)
                    return

                # Check referenced column exists that foreign key references
                reference_key = foreign_key_schema["reference_keys"][i]
                if not check_column_exist(reference_key, reference_table_schema):
                    prompt.reference_column_existence_error()
                    return

                # Check reference type
                if get_column_type(foreign_key, table_schema) != get_column_type(reference_key, reference_table_schema):
                    prompt.reference_type_error()
                    return

            # Check foreign key references primary key correctly
            if len(foreign_key_schema["reference_keys"]) != len(reference_table_pk):
                prompt.reference_non_primary_key_error()
                return
            if set(foreign_key_schema["reference_keys"]) != set(reference_table_pk):
                prompt.reference_non_primary_key_error()
                return
            table_schema["foreign_key"].append(foreign_key_schema)

        table_schema_json = json.dumps(table_schema)
        myDB.put(table_name.encode('utf-8'), table_schema_json.encode('utf-8'))
        prompt.create_table_success(table_name)

    def drop_table_query(self, items):
        table_name = items[2].children[0].lower()
        table_schema = get_table_schema(myDB, table_name)
        # Check if table exists
        if table_schema is None:
            prompt.no_such_table()
            return

        # Get table schema and delete from DB file
        db_cursor = myDB.cursor()
        while x := db_cursor.next():
            temp_schema = json.loads(x[1])
            for reference_schema in temp_schema["foreign_key"]:
                if reference_schema["reference_table"].lower() == table_name:
                    prompt.drop_referenced_table_error(table_name)
                    return
        if os.path.exists(f"DB/{table_name}.db"):
            os.remove(f"DB/{table_name}.db")
        myDB.delete(table_name.encode('utf-8'))
        prompt.drop_success(table_name)

    def explain_query(self, items):
        table_name = items[1].children[0].lower()
        table_schema = get_table_schema(myDB, table_name)
        # Check table exists
        if table_schema is None:
            prompt.no_such_table()
            return

        column_names = []
        types = []
        nullables = []

        # Make lists to get max_length of each column
        for column_def in table_schema["columns"]:
            column_names.append(column_def["name"])
            types.append(column_def["type"])
            nullables.append('Y' if column_def["nullable"] else 'N')

        cn_max = max(len(x) for x in column_names)
        if cn_max < len(f"table_name [{table_name}]"):
            cn_max = len(f"table_name [{table_name}]")
        ty_max = max(len(x) for x in types)
        if ty_max < 4:
            ty_max = 4

        # Print with alignment
        length = cn_max + ty_max + 24
        print("-" * length)
        print(f"table_name [{table_name}]")
        print("column_name".ljust(cn_max) + '\t', end="")
        print("type".ljust(ty_max) + '\t', end="")
        print("null".ljust(12), end="")
        print("key")

        for column_def in table_schema["columns"]:
            print(column_def["name"].ljust(cn_max) + '\t', end="")
            print(column_def["type"].ljust(ty_max) + '\t', end="")
            print(('Y' if column_def["nullable"] else 'N').ljust(12), end="")
            key_constraint = ""
            if (column_def["name"] in table_schema["primary_key"]):
                key_constraint = "PRI"
            if (is_foreign_key(column_def["name"], table_schema)):
                if key_constraint == "PRI":
                    key_constraint = "PRI/FOR"
                else:
                    key_constraint = "FOR"
            print(key_constraint)
        print("-" * length)

    def describe_query(self, items):
        MyTransformer(myDB, prompt).explain_query(items)

    def desc_query(self, items):
        MyTransformer(myDB, prompt).explain_query(items)

    def insert_query(self, items):
        table_name = items[2].children[0].lower()

        table_schema = get_table_schema(myDB, table_name)
        # Check table exists
        if table_schema is None:
            prompt.no_such_table()
            return

        # if items[3] is None --> 현재 상황

        # values have the raw values and primary_key_values have list that consists of only primary key values
        values = []
        for inserted_value_tree in items[5].children[1:-1]:
            values.append(eval(inserted_value_tree.children[0].children[0].lower()))

        primary_key_values = []
        for i in range(len(values)):
            column_def = table_schema["columns"][i]
            if column_def["type"].startswith("char"):
                length = get_char_type_len(column_def["type"])
                values[i] = values[i][:length]
            if column_def["name"] in table_schema["primary_key"]:
                primary_key_values.append(values[i])

        insertDB = db.DB()
        insertDB.open(f"DB/{table_name}.db", dbtype=db.DB_HASH, flags=db.DB_CREATE)

        pkv_json = json.dumps(primary_key_values)
        val_json = json.dumps(values)
        insertDB.put(pkv_json.encode('utf-8'), val_json.encode('utf-8'))

        insertDB.close()
        prompt.insert_result()

    def delete_query(self, items):
        print("'DELETE' requested")

    def select_query(self, items):
        table_name = next(i for i in items[2].find_data("table_name")).children[0].lower()
        if myDB.get(table_name.encode('utf-8')) is None:
            prompt.select_table_existence_error(table_name)
            return
        table_schema = get_table_schema(myDB, table_name)

        # column_to_get has indices of each column, and in this project, all indices.
        column_to_get = []
        column_names = []
        if len(items[1].children) == 0:
            for i in range(len(table_schema["columns"])):
                column_to_get.append(i)
                column_names.append(table_schema["columns"][i]['name'])

        raw_values = []

        selectDB = db.DB()
        selectDB.open(f"DB/{table_name}.db", dbtype=db.DB_HASH, flags=db.DB_CREATE)

        db_cursor = selectDB.cursor()
        while x := db_cursor.next():
            raw_values.append(json.loads(x[1].decode('utf-8')))

        values = []
        for raw_value in raw_values:
            value = [raw_value[x] for x in column_to_get]
            values.append(value)

        # to print aligned
        max_lengths = [len(column_name) for column_name in column_names]

        for row in values:
            for i, value in enumerate(row):
                max_lengths[i] = max(max_lengths[i], len(str(value)))

        for i in range(len(max_lengths)):
            max_lengths[i] += 2

        for max_length in max_lengths:
            print('+' + '-' * max_length, end="")
        print('+')

        for i, max_length in enumerate(max_lengths):
            print('| ' + column_names[i].upper().ljust(max_length - 1), end="")
        print('|')

        for max_length in max_lengths:
            print('+' + '-' * max_length, end="")
        print('+')

        for value in values:
            for i, max_length in enumerate(max_lengths):
                print('| ' + str(value[i]).ljust(max_length - 1), end="")
            print('|')

        for max_length in max_lengths:
            print('+' + '-' * max_length, end="")
        print('+')

    def show_tables_query(self, items):
        # get all table_name
        print("------------------------")
        db_cursor = myDB.cursor()
        while x := db_cursor.next():
            print(x[0].decode('utf-8'))
        print("------------------------")

    def update_query(self, items):
        print("'UPDATE' requested")

    def EXIT(self, items):
        myDB.close()
        exit()


with open('grammar.lark') as file:
    sql_parser = Lark(file.read(), start="command", lexer="basic")

if not os.path.exists('DB'):
    os.mkdir('DB')

myDB = db.DB()
myDB.open('DB/myDB.db', dbtype=db.DB_HASH, flags=db.DB_CREATE)

prompt = Prompt()

while True:
    try:
        raw_input = prompt.get_input()
        while not raw_input.endswith(';'):  # input always ends with semicolon
            raw_input += '\n' + input()

        commands = raw_input.split(';')[:-1]  # last element is empty string
        for command in commands:
            command += ';'  # attaching semicolon removed by split
            output = sql_parser.parse(command)  # lark parses output
            MyTransformer(prompt, myDB).transform(output)  # transform() runs the function that correspond to statement
    # lark raises exceptions inheriting LarkError, so catch LarkError for every exception
    except lark.exceptions.LarkError as e:
        prompt.syntax_error()
