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

    def insert_result(self):
        Prompt().show_prompt()
        print("1 row inserted")

    def insert_type_mismatch_error(self):
        Prompt().show_prompt()
        print("Insertion has failed: Types are not matched")

    def insert_column_existence_error(self, col_name):
        Prompt().show_prompt()
        print("Insertion has failed: '%s' does not exist" % col_name)

    def insert_column_non_nullable_error(self, col_name):
        Prompt().show_prompt()
        print("Insertion has failed: '%s' is not nullable" % col_name)

    def delete_result(self, count):
        Prompt().show_prompt()
        print("%d row(s) deleted" % count)

    def select_table_existence_error(self, table_name):
        Prompt().show_prompt()
        print("Selection has failed: '%s' does not exist" % table_name)

    def select_column_resolve_error(self, col_name):
        Prompt().show_prompt()
        print("Selection has failed: fail to resolve '%s'" % col_name)

    def where_incomparable_error(self):
        Prompt().show_prompt()
        print("Where clause trying to compare incomparable values")

    def where_table_not_specified(self):
        Prompt().show_prompt()
        print("Where clause trying to reference tables which are not specified")

    def where_column_not_exist(self):
        Prompt().show_prompt()
        print("Where clause trying to reference non existing column")

    def where_ambiguous_reference(self):
        Prompt().show_prompt()
        print("Where clause containes ambiguous reference")

    def insert_duplicate_primary_key_error(self):
        Prompt().show_prompt()
        print("Insertion has failed: Primary key duplication")

    def insert_referential_integrity_error(self):
        Prompt().show_prompt()
        print("Insertion has failed: Referential integrity violation")

    def delete_referential_integrity_passed(self, count):
        Prompt().show_prompt()
        print("%d row(s) are not deleted due to referential integrity" % count)


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
    match = re.search(r'[-+]?\d+', type)
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


def is_type_match(scheme_type, input_type):
    if input_type == "NULL":
        return True
    elif scheme_type.startswith("char") and input_type == "STR":
        return True
    elif scheme_type == "date" and input_type == "DATE":
        return True
    elif scheme_type == "int" and input_type == "INT":
        return True
    else:
        return False


def check_where_clause_type_match(comp_operands, comp_op):
    for i in range(len(comp_operands)):
        if comp_operands[i].startswith("char"):
            comp_operands[i] = "str"
        comp_operands[i] = comp_operands[i].lower()
    if comp_operands[0] != comp_operands[1]:
        return False
    if comp_operands[0] == "str" and (comp_op != "=" and comp_op != "!="):
        return False
    return True


def is_valid_where_clause(table_schemas, where_clause, prompt):
    for predicate in where_clause.find_data("predicate"):
        if predicate.children[0].data == "comparison_predicate":
            comp_op = next(i for i in predicate.find_data("comp_op")).children[0].value
            comp_operand_type_list = []
            for comp_operand in predicate.find_data("comp_operand"):
                if len(comp_operand.children) == 2:  # operand is column
                    # no table in schemas
                    if comp_operand.children[0] is not None:
                        table_name = comp_operand.children[0].children[0].value
                        if not any(table_schema["table_name"] == table_name for table_schema in table_schemas):
                            prompt.where_table_not_specified()
                            return False

                    # table exists or no table
                    # column exist check
                    # if table name specified
                    if comp_operand.children[0] is not None:
                        table_name = comp_operand.children[0].children[0].value
                        table_schema = {}
                        for table in table_schemas:
                            if table["table_name"] == table_name:
                                table_schema = table
                                break
                        if not any(column_def["name"] == comp_operand.children[1].children[0].value for column_def in
                                   table_schema["columns"]):
                            prompt.where_column_not_exist()
                            return False
                        for column_def in table_schema["columns"]:
                            if comp_operand.children[1].children[0].value == column_def["name"]:
                                comp_operand_type_list.append(column_def["type"])
                                break
                    # not specified
                    else:
                        column_cnt = 0
                        for table_schema in table_schemas:
                            if any(
                                    column_def["name"] == comp_operand.children[1].children[0].value for column_def in
                                    table_schema["columns"]):
                                column_cnt += 1
                        if column_cnt == 0:
                            prompt.where_column_not_exist()
                            return False
                        elif column_cnt > 1:
                            prompt.where_ambiguous_reference()
                            return False
                        for table_schema in table_schemas:
                            for column_def in table_schema["columns"]:
                                if comp_operand.children[1].children[0].value == column_def["name"]:
                                    comp_operand_type_list.append(column_def["type"])
                                    break

                else:  # operand is comparable
                    comp_operand_type_list.append(comp_operand.children[0].children[0].type)

            if not check_where_clause_type_match(comp_operand_type_list, comp_op):
                prompt.where_incomparable_error()
                return False
    return True


def compare(comp_operands, comp_op):
    if comp_operands[0][0].lower() == 'null' or comp_operands[1][0].lower() == 'null':
        return False
    if comp_op == "=":
        return comp_operands[0][1] == comp_operands[1][1]
    elif comp_op == "!=":
        return comp_operands[0][1] != comp_operands[1][1]
    elif comp_op == ">":
        return comp_operands[0][1] > comp_operands[1][1]
    elif comp_op == ">=":
        return comp_operands[0][1] >= comp_operands[1][1]
    elif comp_op == "<":
        return comp_operands[0][1] < comp_operands[1][1]
    elif comp_op == "<=":
        return comp_operands[0][1] <= comp_operands[1][1]
    else:
        return False


def where_clauses_match(where_clause, table_schemas, record):
    log_op = None
    if len(list(where_clause.find_data("predicate"))) == 2:
        # naive approach,
        if len(list(where_clause.find_data("boolean_expr"))[0].children) == 1:
            log_op = "and"
        else:
            log_op = "or"

    booleans = []

    for predicate in where_clause.find_data("predicate"):
        # comparison_predicate
        if predicate.children[0].data == "comparison_predicate":
            comp_op = next(i for i in predicate.find_data("comp_op")).children[0].value
            comp_operands = []
            for comp_operand in predicate.find_data("comp_operand"):
                if len(comp_operand.children) == 2:  # operand is column
                    # table name specified
                    if comp_operand.children[0] is not None:
                        table_name = comp_operand.children[0].children[0].value
                        for table_schema in table_schemas:
                            if table_schema["table_name"] == table_name:
                                for i, column_def in enumerate(table_schema["columns"]):
                                    if column_def["name"] == comp_operand.children[1].children[0].value:
                                        comp_operands.append(record[table_name][i])
                    # table name not specified
                    else:
                        for table_schema in table_schemas:
                            for i, column_def in enumerate(table_schema["columns"]):
                                if column_def["name"] == comp_operand.children[1].children[0].value:
                                    comp_operands.append(record[table_schema["table_name"]][i])

                # operand is comparable value
                else:
                    comp_operand_token = comp_operand.children[0].children[0]
                    if comp_operand_token.type == 'STR':
                        comp_operands.append([comp_operand_token.type, eval(comp_operand_token.value).lower()])
                    elif comp_operand_token.type == 'INT':
                        comp_operands.append([comp_operand_token.type, eval(comp_operand_token.value)])
                    else:
                        comp_operands.append([comp_operand_token.type, comp_operand_token.value])

            booleans.append(compare(comp_operands, comp_op))
        # null predicate
        else:
            # table specified
            col_name = predicate.children[0].children[1].children[0].value
            not_exists = predicate.children[0].children[2].children[1] is not None
            if predicate.children[0].children[0] is not None:
                table_name = predicate.children[0].children[0].children[0].value
                for table_schema in table_schemas:
                    if table_schema["table_name"] == table_name:
                        for i, column_def in enumerate(table_schema["columns"]):
                            if column_def["name"] == col_name:
                                if not not_exists:
                                    booleans.append(record[table_name][i][1] == "null")
                                else:
                                    booleans.append(record[table_name][i][1] != "null")
            # table not specified
            else:
                for table_schema in table_schemas:
                    for i, column_def in enumerate(table_schema["columns"]):
                        if column_def["name"] == col_name:
                            if not not_exists:
                                booleans.append(record[table_schema["table_name"]][i][1] == "null")
                            else:
                                booleans.append(record[table_schema["table_name"]][i][1] != "null")

    if log_op is None:
        return booleans[0]
    elif log_op == "and":
        return booleans[0] and booleans[1]
    elif log_op == "or":
        return booleans[0] or booleans[1]
    else:
        return False


def cartesian_product(lists):
    if not lists:
        return [{}]

    result = []
    for product_rest in cartesian_product(lists[1:]):
        for item in lists[0]:
            temp = {}
            temp.update(item)
            temp.update(product_rest)
            result.append(temp)

    return result

def check_delete_fk_constraints(table_schema, referenced_tuples, record):
    for col, datas in referenced_tuples.items():
        col_list = col.split(",")
        record_in_cols = []
        for col_name in col_list:
            for i, column_def in enumerate(table_schema["columns"]):
                if col_name == column_def["name"]:
                    record_in_cols.append(record[i])
        if record_in_cols in datas:
            return False
    return True


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

        # input column not specified
        if items[3] is None:
            col_names = [x["name"] for x in table_schema["columns"]]
        # input column specified
        else:
            col_names = [x.children[0].value.lower() for x in items[3].children[1:-1]]

        # check column existence
        for col_name in col_names:
            found = any(column["name"] == col_name for column in table_schema["columns"])
            if not found:
                prompt.insert_column_existence_error(col_name)
                return

        col_infos = []
        for x in table_schema["columns"]:
            if x["name"] in col_names:
                col_infos.append((x["type"], x["name"]))

        # values have the raw values and primary_key_values have list that consists of only primary key values
        values = []
        for x in items[5].children[1:-1]:
            if type(x.children[0]) == lark.Token:
                values.append(x.children[0])
            else:
                values.append(x.children[0].children[0])

        # insert type match check
        if len(col_infos) != len(values):
            prompt.insert_type_mismatch_error()
            return

        for i, col_info in enumerate(col_infos):
            if not is_type_match(col_info[0], values[i].type):
                prompt.insert_type_mismatch_error()
                return

        primary_key_values = []
        table_values = []
        for i in range(len(table_schema["columns"])):
            column_def = table_schema["columns"][i]

            if (column_def["type"], column_def["name"]) in col_infos:
                idx = col_infos.index((column_def["type"], column_def["name"]))
                table_values.append([values[idx].type, values[idx].value])
            else:
                table_values.append(["NULL", "null"])

            # nullable check
            if column_def["nullable"] is False and table_values[i][0].lower() == "null":
                prompt.insert_column_non_nullable_error(column_def["name"])
                return

            # type check
            if not table_values[i][1] == "null":
                # char type check
                if column_def["type"].startswith("char"):
                    length = get_char_type_len(column_def["type"])
                    table_values[i][1] = eval(table_values[i][1])[:length].lower()
                # int type check
                elif column_def["type"] == "int":
                    table_values[i][1] = eval(table_values[i][1])
            if column_def["name"] in table_schema["primary_key"]:
                primary_key_values.append(table_values[i])

        # foreign key constraints check
        foreign_key_constraints = table_schema["foreign_key"]
        for fk_constraint in foreign_key_constraints:
            reference_table_name = fk_constraint["reference_table"]

            foreign_key_values = []
            for fk_column in fk_constraint["foreign_keys"]:
                for i, column_def in enumerate(table_schema["columns"]):
                    if column_def["name"] == fk_column:
                        foreign_key_values.append(table_values[i])

            refernceDB = db.DB()
            refernceDB.open(f"DB/{reference_table_name}.db", dbtype=db.DB_HASH, flags=db.DB_CREATE)
            if refernceDB.get(json.dumps(foreign_key_values).encode("utf-8")) is None:
                refernceDB.close()
                prompt.insert_referential_integrity_error()
                return
            refernceDB.close()


        insertDB = db.DB()
        insertDB.open(f"DB/{table_name}.db", dbtype=db.DB_HASH, flags=db.DB_CREATE)

        # if no primary key
        if len(primary_key_values) == 0:
            db_cursor = insertDB.cursor()
            max_key = '0'
            while x := db_cursor.next():
                max_key = max(max_key, x[0].decode("utf-8"))
            pkv_json = max_key
        else:
            pkv_json = json.dumps(primary_key_values)
        val_json = json.dumps(table_values)

        if insertDB.get(pkv_json.encode('utf-8')) is not None:
            prompt.insert_duplicate_primary_key_error()
            insertDB.close()
            return

        insertDB.put(pkv_json.encode('utf-8'), val_json.encode('utf-8'))

        insertDB.close()
        prompt.insert_result()

    def delete_query(self, items):
        table_name = items[2].children[0].lower()

        table_schema = get_table_schema(myDB, table_name)
        # Check table exists
        if table_schema is None:
            prompt.no_such_table()
            return

        referenced_tuples = {}

        # check referential integrity
        mydb_cursor = myDB.cursor()
        while x := mydb_cursor.next():
            child_table_schema = json.loads(x[1].decode("utf-8"))
            if child_table_schema["table_name"] != table_name:
                for fk_constraint in child_table_schema["foreign_key"]:
                    if fk_constraint["reference_table"] == table_name:
                        referenced_tuples[",".join(fk_constraint["reference_keys"])] = []
                        childDB = db.DB()
                        childDB.open(f"DB/{child_table_schema['table_name']}.db", dbtype=db.DB_HASH, flags=db.DB_CREATE)
                        child_cursor = childDB.cursor()
                        while child_x := child_cursor.next():
                            child_data_row = json.loads(child_x[1].decode("utf-8"))
                            single_data = []
                            for column_name in fk_constraint["foreign_keys"]:
                                for i, column_def in enumerate(child_table_schema["columns"]):
                                    if column_def["name"] == column_name:
                                        single_data.append(child_data_row[i])
                            referenced_tuples[",".join(fk_constraint["reference_keys"])].append(single_data)
                        childDB.close()

        deleted_items = 0
        deleteDB = db.DB()
        deleteDB.open(f"DB/{table_name}.db", dbtype=db.DB_HASH, flags=db.DB_CREATE)
        db_cursor = deleteDB.cursor()

        # no where clause
        if items[3] is None:
            count = 0
            while x := db_cursor.next():
                if not check_delete_fk_constraints(table_schema, referenced_tuples, json.loads(x[1].decode("utf-8"))):
                    count += 1
            if count != 0:
                prompt.delete_referential_integrity_passed(count)
                return
            else:
                db_cursor = deleteDB.cursor()
                while x := db_cursor.next():
                    deleteDB.delete(x[0])
                    deleted_items += 1
            prompt.delete_result(deleted_items)
        # where clause exists
        else:
            table_schemas = [table_schema]
            where_clause = items[3]
            if not is_valid_where_clause(table_schemas, where_clause, prompt):
                return

            count = 0
            while x := db_cursor.next():
                if where_clauses_match(where_clause, table_schemas,
                                       {table_schema["table_name"]: json.loads(x[1].decode("utf-8"))}):
                    if not check_delete_fk_constraints(table_schema, referenced_tuples, json.loads(x[1].decode("utf-8"))):
                        count += 1
            if count != 0:
                prompt.delete_referential_integrity_passed(count)
                return
            else:
                db_cursor = deleteDB.cursor()
                while x := db_cursor.next():
                    if where_clauses_match(where_clause, table_schemas,
                                           {table_schema["table_name"]: json.loads(x[1].decode("utf-8"))}):
                        deleteDB.delete(x[0])
                        deleted_items += 1
            prompt.delete_result(deleted_items)

    def select_query(self, items):

        table_names = list(i.children[0].lower() for i in items[2].children[0].find_data("table_name"))

        table_schemas = []
        for table_name in table_names:
            if myDB.get(table_name.encode('utf-8')) is None:
                prompt.select_table_existence_error(table_name)
                return
            table_schemas.append(get_table_schema(myDB, table_name))

        column_names = []
        for table_schema in table_schemas:
            for column_def in table_schema["columns"]:
                column_names.append(".".join([table_schema["table_name"].lower(), column_def["name"].lower()]))

        selected_columns = []
        printed_selected_columns = []
        # select [columns]
        if len(items[1].children) > 0:
            for selected_column in items[1].children:
                # table not specified
                if selected_column.children[0] is None:
                    selected_column_name = selected_column.children[1].children[0].value
                    column_count = 0
                    for table_schema in table_schemas:
                        if any(column_def["name"] == selected_column_name.lower() for column_def in table_schema["columns"]):
                            column_count += 1
                        for column_def in table_schema["columns"]:
                            if column_def["name"] == selected_column_name.lower():
                                selected_columns.append([table_schema["table_name"], column_def["name"]])
                                printed_selected_columns.append(column_def["name"])
                    if column_count != 1:
                        prompt.select_column_resolve_error(selected_column_name)
                        return
                # table specified
                else:
                    selected_table_name = selected_column.children[0].children[0].value
                    selected_column_name = selected_column.children[1].children[0].value
                    if not any(table_schema["table_name"] == selected_table_name.lower() for table_schema in table_schemas):
                        prompt.select_column_resolve_error(selected_table_name)
                        return
                    selected_columns.append([selected_table_name.lower(), selected_column_name.lower()])
                    printed_selected_columns.append(".".join(selected_columns[-1]))
        # select *
        else:
            for i in range(len(column_names)):
                selected_columns.append(column_names[i].split("."))
                if len(table_schemas) == 1:
                    printed_selected_columns.append(column_names[i].split(".")[1])
                else:
                    printed_selected_columns.append(column_names[i])

        selectDBs = []
        select_db_datas = []
        for i, table_schema in enumerate(table_schemas):
            selectDBs.append(db.DB())
            selectDBs[i].open(f"DB/{table_schema['table_name']}.db", dbtype=db.DB_HASH, flags=db.DB_CREATE)
            db_cursor = selectDBs[i].cursor()
            select_db_datas.append([])
            while x := db_cursor.next():
                select_db_datas[i].append({table_schema["table_name"]: json.loads(x[1].decode("utf-8"))})

        # cartesian product tables
        datas = cartesian_product(select_db_datas)

        selected_column_indexes = []
        for selected_column in selected_columns:
            for table_schema in table_schemas:
                if table_schema["table_name"] == selected_column[0]:
                    for i in range(len(table_schema["columns"])):
                        if table_schema["columns"][i]["name"] == selected_column[1]:
                            selected_column_indexes.append([selected_column[0], i])

        selected_datas = []
        # where clause
        if len(list(items[2].find_data("where_clause"))):
            where_clause = next(i for i in items[2].find_data("where_clause"))
            if not is_valid_where_clause(table_schemas, where_clause, prompt):
                return

            for data in datas:
                if where_clauses_match(where_clause, table_schemas, data):
                    selected_data = []
                    for selected_column_index in selected_column_indexes:
                        selected_data.append(data[selected_column_index[0]][selected_column_index[1]][1])
                    selected_datas.append(selected_data)
        # no where clause
        else:
            for data in datas:
                selected_data = []
                for selected_column_index in selected_column_indexes:
                    selected_data.append(data[selected_column_index[0]][selected_column_index[1]][1])
                selected_datas.append(selected_data)

        # print with alignments
        max_lengths = [len(printed_selected_column) for printed_selected_column in printed_selected_columns]

        for row in selected_datas:
            for i, value in enumerate(row):
                max_lengths[i] = max(max_lengths[i], len(str(value)))

        for i in range(len(max_lengths)):
            max_lengths[i] += 2

        for max_length in max_lengths:
            print('+' + '-' * max_length, end="")
        print('+')

        for i, max_length in enumerate(max_lengths):
            print('| ' + printed_selected_columns[i].upper().ljust(max_length - 1), end="")
        print('|')

        for max_length in max_lengths:
            print('+' + '-' * max_length, end="")
        print('+')

        for selected_data in selected_datas:
            for i, max_length in enumerate(max_lengths):
                print('| ' + str(selected_data[i]).ljust(max_length - 1), end="")
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
        # print(e)
        prompt.syntax_error()
