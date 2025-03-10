from mysql.connector import connect

connection = connect(
    host='astronaut.snu.ac.kr',
    port=7000,
    user='DB2020_12852',
    password='DB2020_12852',
    db='DB2020_12852',
    charset='utf8'
)

with connection.cursor(dictionary=True) as cursor:
    cursor.execute("drop table reservations")
    cursor.execute("drop table movies")
    cursor.execute("drop table users")

    cursor.execute("create table movies(movie_id int not null auto_increment primary key, movie_title varchar(255) not null, movie_director varchar(255) not null, price int not null, unique key (movie_title, movie_director));")
    cursor.execute("create table users(user_id int not null auto_increment primary key, user_name varchar(255) not null, user_age int not null, unique key (user_name, user_age));")
    cursor.execute("create table reservations(movie_id int, user_id int, foreign key (movie_id) references movies(movie_id) on delete cascade, foreign key (user_id) references users(user_id) on delete cascade);")
    # connection.commit()
    # cursor.execute("show tables;")
    # result = cursor.fetchall()
    # cursor.execute("desc movies;")
    # result += cursor.fetchall()
    # cursor.execute("desc clients;")
    # result += cursor.fetchall()
    # cursor.execute("desc reservations;")
    # result += cursor.fetchall()
    # cursor.execute("delete from movies")
    connection.commit()
    result = cursor.fetchall()
    print(result)

connection.close()