from mysql.connector import connect, errors
import pandas as pd
import numpy as np

connection = connect(
    host='astronaut.snu.ac.kr',
    port=7000,
    user='DB2020_12852',
    password='DB2020_12852',
    db='DB2020_12852',
    charset='utf8'
)
cursor = connection.cursor(dictionary=True)

'''
1번 실행될때 에러 고려
'''

'''
results에 있는 내용 형식 맞춰 출력
'''
def print_results_in_table(results):
    # header list
    header = list(results[0].keys())

    # length 구하기
    length = [len(h) for h in header]
    for result in results:
        for i in range(len(header)):
            if length[i] < len(str(result[header[i]])):
                length[i] = len(str(result[header[i]]))

    length = [l + 4 for l in length]

    # table 출력
    print("-" * sum(length))
    header_str = ""
    for i in range(len(header)):
        header_str += header[i].ljust(length[i])
    print(header_str)
    print("-" * sum(length))
    for result in results:
        item_str = ""
        if not all(value is None for value in result.values()):
            for i in range(len(header)):
                item_str += str(result[header[i]]).ljust(length[i])

            print(item_str)
    print("-" * sum(length))

'''
string을 int로 바꾸면서 casting error 등 처리
'''
def str_to_int(string):
    try:
        integer = int(string)
    except ValueError:
        return None
    return integer

'''
title로 movie의 select문 실행
'''
def get_movie(title):
    cursor.execute("SELECT * FROM movies WHERE movie_title = %s", [title])
    return cursor.fetchall()

'''
movie_id로 select문 실행
'''
def get_movie_by_id(movie_id):
    cursor.execute("SELECT * FROM movies WHERE movie_id = %s", [movie_id])
    return cursor.fetchall()

'''
name, age로 user select문 실행
'''
def get_user(name, age):
    cursor.execute("SELECT * FROM users WHERE user_name = %s and user_age = %s", (name, age))
    return cursor.fetchall()

'''
user_id로 select문 실행
'''
def get_user_by_id(user_id):
    cursor.execute("SELECT * FROM users WHERE user_id = %s", [user_id])
    return cursor.fetchall()


'''
DB에 영화 추가
'''
def insert_movie_into_db(title, director, price):
    if len(get_movie(title)) != 0:
        return False
    else:
        try:
            cursor.execute("INSERT INTO movies (movie_title, movie_director, price) VALUES (%s, %s, %s)",
                           (title, director, price))
            connection.commit()
            return True
        # Cannot reach here: duplicate insertion
        except errors.IntegrityError:
            pass


'''
DB에 사용자 추가
'''
def insert_user_into_db(name, age):
    if len(get_user(name, age)) != 0:
        return False
    else:
        try:
            cursor.execute("INSERT INTO users (user_name, user_age) VALUES (%s, %s)",
                           (name, age))
            connection.commit()
            return True
        # Cannot reach here: duplicate insertion
        except errors.IntegrityError:
            pass

def insert_reservation_into_db(movie_id, user_id):
    # DB 각각에서 영화, 사용자 존재 여부 확인
    if movie_id is None or len(get_movie_by_id(movie_id)) == 0:
        print(f'Movie {movie_id} does not exist')
        return False
    if user_id is None or len(get_user_by_id(user_id)) == 0:
        print(f'User {user_id} does not exist')
        return False

    # 이미 예매했는지 확인
    cursor.execute("SELECT * FROM reservations WHERE movie_id = %s and user_id = %s", (movie_id, user_id))
    if len(cursor.fetchall()) != 0:
        print(f'User {user_id} already booked movie {movie_id}')
        return False
    # 영화에 10명 이상 예매했는지 확인
    cursor.execute("SELECT COUNT(user_id) AS COUNT FROM reservations WHERE movie_id = %s", [movie_id])
    if cursor.fetchall()[0]['COUNT'] >= 10:
        print(f'Movie {movie_id} has already been fully booked')
        return False
    # 예매 정보 저장
    else:
        cursor.execute("INSERT INTO reservations VALUES (%s, %s, null)", (movie_id, user_id))
        connection.commit()
        return True


# Problem 1 (5 pt.)
def initialize_database():
    # YOUR CODE GOES HERE
    data = pd.read_csv('data.csv')
    cursor = connection.cursor()

    movie_set = set()
    user_set = set()

    #각각 중복 체크 후 DB에 삽입
    for row in data.itertuples():
        if not row[1:4] in movie_set:
            title, director, price = row[1:4]
            insert_movie_into_db(title, director, price)
        if not row[4:6] in user_set:
            name, age = row[4:6]
            insert_user_into_db(name, age)
        movie_set.add(row[1:4])
        user_set.add(row[4:6])

    #이후 reservation 정보 저장.
    for row in data.itertuples():
        movie_id = get_movie(row[1])[0]['movie_id']
        user_id = get_user(row[4], row[5])[0]['user_id']
        insert_reservation_into_db(movie_id, user_id)

    connection.commit()
    print('Database successfully initialized')
    # YOUR CODE GOES HERE
    pass


# Problem 15 (5 pt.)
def reset():
    # YOUR CODE GOES HERE
    confirm = input("reset?(y/n): ")
    # 테이블 다 삭제 후 다시 생성
    if confirm == 'Y' or confirm == 'y':
        cursor.execute("drop table reservations")
        cursor.execute("drop table movies")
        cursor.execute("drop table users")

        cursor.execute(
            "create table movies("
            "movie_id int not null auto_increment primary key, "
            "movie_title varchar(255) not null, "
            "movie_director varchar(255) not null, "
            "price int not null, "
            "unique key (movie_title))"
        )
        cursor.execute(
            "create table users("
            "user_id int not null auto_increment primary key, "
            "user_name varchar(255) not null, "
            "user_age int not null, "
            "unique key (user_name, user_age))"
        )
        cursor.execute(
            "create table reservations("
            "movie_id int not null, "
            "user_id int not null, "
            "rating int,"
            "foreign key (movie_id) references movies(movie_id) on delete cascade, "
            "foreign key (user_id) references users(user_id) on delete cascade, "
            "unique key (movie_id, user_id))"
        )

        # initialize_database()
    elif confirm == 'N' or confirm == 'n':
        pass
    else:
        print("Invalid action")
    # YOUR CODE GOES HERE
    pass


# Problem 2 (4 pt.)
def print_movies():
    # YOUR CODE GOES HERE
    # select문을 이용해 movie와 reservation join한 뒤 출력
    cursor.execute(
        "SELECT movie_id AS id, movie_title AS title, movie_director as director, price, reservation, `avg. rating` "
        "FROM movies LEFT JOIN "
        "(SELECT movie_id, COUNT(user_id) AS reservation , AVG(rating) AS 'avg. rating' FROM reservations GROUP BY movie_id) "
        "AS MOV_INFO USING(movie_id)"
        "ORDER BY movie_id"
    )
    results = cursor.fetchall()
    if len(results) == 0:
        results.append(
            {'id': None, 'title': None, 'director': None, 'price': None, 'reservation': None, 'avg. rating': None})
    print_results_in_table(results)

    # YOUR CODE GOES HERE
    pass


# Problem 3 (4 pt.)
def print_users():
    # YOUR CODE GOES HERE
    # select문을 이용해 users 출력
    cursor.execute("SELECT user_id AS id, user_name AS name, user_age AS age "
                   "FROM users ORDER BY user_id")
    results = cursor.fetchall()

    if len(results) == 0:
        results.append({'id': None, 'name': None, 'age': None})
    print_results_in_table(results)
    # YOUR CODE GOES HERE
    pass


# Problem 4 (4 pt.)
def insert_movie():
    # YOUR CODE GOES HERE
    title = input('Movie title: ')
    director = input('Movie director: ')
    price = input('Movie price: ')

    price = str_to_int(price)
    if price is None or not 0 <= price <= 100000:
        print('Movie price should be from 0 to 100000')
        return

    if insert_movie_into_db(title, director, price):
        print('One movie successfully inserted')
    else:
        print(f'Movie {title} already exists')
    # YOUR CODE GOES HERE
    pass


# Problem 6 (4 pt.)
def remove_movie():
    # YOUR CODE GOES HERE
    movie_id = input('Movie ID: ')

    movie_id = str_to_int(movie_id)
    # movie 정보가 DB에 없을 때
    if movie_id is None or len(get_movie_by_id(movie_id)) == 0:
        print(f'Movie {movie_id} does not exist')
        return
    else:
        cursor.execute("DELETE FROM movies WHERE movie_id = %s", [movie_id])
        connection.commit()
        print('One movie successfully removed')
    # YOUR CODE GOES HERE
    pass


# Problem 5 (4 pt.)
def insert_user():
    # YOUR CODE GOES HERE
    name = input('User name: ')
    age = input('User age: ')

    age = str_to_int(age)
    if age is None or not 12 <= age <= 110:
        print('User age should be from 12 to 110')
        return

    if insert_user_into_db(name, age):
        print('One user successfully inserted')
    else:
        print(f'User ({name}, {age}) already exists')

    # YOUR CODE GOES HERE
    pass


# Problem 7 (4 pt.)
def remove_user():
    # YOUR CODE GOES HERE
    user_id = input('User ID: ')

    user_id = str_to_int(user_id)

    # user 정보가 DB에 없을 때
    if user_id is None or len(get_user_by_id(user_id)) == 0:
        print(f'User {user_id} does not exist')
        return
    cursor.execute("DELETE FROM users WHERE user_id = %s", [user_id])
    connection.commit()
    print('One user successfully removed')
    # YOUR CODE GOES HERE
    pass


# Problem 8 (5 pt.)
def book_movie():
    # YOUR CODE GOES HERE
    movie_id = input('Movie ID: ')
    user_id = input('User ID: ')

    if insert_reservation_into_db(movie_id, user_id):
        print('Movie successfully booked')

    # YOUR CODE GOES HERE
    pass


# Problem 9 (5 pt.)
def rate_movie():
    # YOUR CODE GOES HERE
    movie_id = input('Movie ID: ')
    user_id = input('User ID: ')
    rating = input('Ratings (1~5): ')

    movie_id = str_to_int(movie_id)
    user_id = str_to_int(user_id)
    rating = str_to_int(rating)

    if movie_id is None or len(get_movie_by_id(movie_id)) == 0:
        print(f'Movie {movie_id} does not exist')
        return
    if user_id is None or len(get_user_by_id(user_id)) == 0:
        print(f'User {user_id} does not exist')
        return

    if rating is None or not 1 <= rating <= 5:
        print(f'Wrong value for a rating')
        return

    # 예매 정보가 있는지 확인 후 있다면 rating 확인, 이후 추가
    cursor.execute("SELECT * FROM reservations WHERE movie_id = %s and user_id = %s", (movie_id, user_id))
    reservation = cursor.fetchall()
    if len(reservation) == 0:
        print(f'User {user_id} has not booked movie {movie_id} yet')
        return
    elif reservation[0]['rating'] is not None:
        print(f'User {user_id} has already rated movie {movie_id}')
        return
    else:
        cursor.execute("UPDATE reservations SET rating = %s WHERE movie_id = %s and user_id = %s",
                       (rating, movie_id, user_id))
        connection.commit()
        print('Movie successfully rated')
    # YOUR CODE GOES HERE
    pass


# Problem 10 (5 pt.)
def print_users_for_movie():
    # YOUR CODE GOES HERE
    movie_id = input('Movie ID: ')

    movie_id = str_to_int(movie_id)
    if movie_id is None or len(get_movie_by_id(movie_id)) == 0:
        print(f'Movie {movie_id} does not exist')
        return

    # 해당 영화를 예매한 모든 사용자 출력
    cursor.execute("SELECT user_id AS id, user_name AS name, user_age AS age, rating "
                   "FROM users NATURAL JOIN reservations "
                   "WHERE movie_id = %s "
                   "ORDER BY user_id",
                   [movie_id])

    results = cursor.fetchall()
    if len(results) == 0:
        results.append({'id': None, 'name': None, 'age': None, 'rating': None})
    print_results_in_table(results)
    # YOUR CODE GOES HERE
    pass


# Problem 11 (5 pt.)
def print_movies_for_user():
    # YOUR CODE GOES HERE
    user_id = input('User ID: ')

    user_id = str_to_int(user_id)
    if user_id is None or len(get_user_by_id(user_id)) == 0:
        print(f'User {user_id} does not exist')
        return

    # 해당 사용자가 예매한 모든 영화 출력
    cursor.execute("SELECT movie_id AS id, movie_title AS title, movie_director AS director, price, rating "
                   "FROM movies NATURAL JOIN reservations "
                   "WHERE user_id = %s "
                   "ORDER BY movie_id",
                   [user_id])

    results = cursor.fetchall()
    if len(results) == 0:
        results.append({'id': None, 'title': None, 'director': None, 'price': None, 'rating': None})
    print_results_in_table(results)
    # YOUR CODE GOES HERE
    pass


# Problem 12 (6 pt.)
def recommend_popularity():
    # YOUR CODE GOES HERE
    user_id = input('User ID: ')

    user_id = str_to_int(user_id)
    if user_id is None or len(get_user_by_id(user_id)) == 0:
        print(f'User {user_id} does not exist')
        return

    # 평균 평점으로 순위매겨 하나만 출력 (사용자가 안 본 영화 중)
    cursor.execute("SELECT m.movie_id AS id, m.movie_title AS title, m.movie_director AS director, m.price, COUNT(r.user_id) as reservation, AVG(r.rating) as 'avg. rating' "
                   "FROM movies m JOIN reservations r USING(movie_id) "
                   "WHERE m.movie_id IN ("
                       "SELECT movie_id "
                       "FROM movies "
                       "WHERE movie_id NOT IN ("
                           "SELECT movie_id FROM reservations "
                           "WHERE user_id = %s "
                        ") "
                    ") "
                    "GROUP BY m.movie_id, m.movie_title, m.movie_director, m.price "
                    "ORDER BY 'avg. rating' DESC, id ASC "
                    "LIMIT 1", [user_id])

    results = cursor.fetchall()
    if len(results) == 0:
        results = {"id": None, 'title': None, 'director': None, 'price': None, 'reservation': None, 'avg. rating': None}
    print("Rating-based")
    print_results_in_table(results)

    # 예매 횟수가 많은 순으로 출력 (사용자가 안 본 영화 중)
    cursor.execute("SELECT m.movie_id AS id, m.movie_title AS title, m.movie_director AS director, m.price, COUNT(r.user_id) as reservation, AVG(r.rating) as 'avg. rating' "
                   "FROM movies m JOIN reservations r USING(movie_id) "
                   "WHERE m.movie_id IN ("
                       "SELECT movie_id "
                       "FROM movies "
                       "WHERE movie_id NOT IN ("
                           "SELECT movie_id FROM reservations "
                           "WHERE user_id = %s "
                        ") "
                    ") "
                    "GROUP BY m.movie_id, m.movie_title, m.movie_director, m.price "
                    "ORDER BY reservation DESC, id ASC "
                    "LIMIT 1", [user_id])

    results = cursor.fetchall()
    if len(results) == 0:
        results = {"id": None, 'title': None, 'director': None, 'price': None, 'reservation': None, 'avg. rating': None}
    print("Popularity-based")
    print_results_in_table(results)

    # YOUR CODE GOES HERE
    pass


# Problem 13 (10 pt.)
def recommend_item_based():
    # YOUR CODE GOES HERE
    user_id = input('User ID: ')

    user_id = str_to_int(user_id)
    if user_id is None or len(get_user_by_id(user_id)) == 0:
        print(f'User {user_id} does not exist')
        return

    cursor.execute("SELECT user_id FROM users ORDER BY user_id")
    user_match = []
    for user_info in cursor.fetchall():
        user_match.append(user_info["user_id"])

    cursor.execute("SELECT movie_id FROM movies ORDER BY movie_id")
    movie_match = []
    for movie_info in cursor.fetchall():
        movie_match.append(movie_info["movie_id"])

    # n, m 구하기
    cursor.execute("SELECT COUNT(user_id) FROM users")
    n = cursor.fetchall()[0]['COUNT(user_id)']
    cursor.execute("SELECT COUNT(movie_id) FROM movies")
    m = cursor.fetchall()[0]['COUNT(movie_id)']

    cursor.execute("SELECT * FROM reservations")
    reservations = cursor.fetchall()

    # 예매 정보로 matrix 업데이트
    user_item_mat = np.zeros((n, m))
    seen_movies = np.zeros(m)
    for reservation in reservations:
        if reservation['rating'] is not None:
            user_item_mat[user_match.index(reservation['user_id'])][movie_match.index(reservation['movie_id'])] = reservation['rating']
        if reservation['user_id'] == user_id:
            seen_movies[movie_match.index(reservation['movie_id'])] = 1

    # rating 정보가 없을 경우
    if all(value == 0 for value in user_item_mat[user_match.index(user_id)]):
        print('Rating does not exist')
        return

    row_conserved = user_item_mat[user_match.index(user_id)]

    row_sums = np.sum(user_item_mat, axis=1)
    row_counts = np.sum(user_item_mat != 0, axis=1)
    row_counts_without_zeros = np.where(row_counts == 0, 1, row_counts)

    # 0을 제외한 값이 있는지 확인 후 평균 계산
    row_means_without_zeros = row_sums / row_counts_without_zeros

    # 0인 칸을 각 행의 평균으로 채우기
    filled_mat = np.where(user_item_mat == 0, row_means_without_zeros.reshape(-1, 1), user_item_mat)

    # 유사도 행렬 계산
    similarity_mat = np.zeros((n, n))
    for i in range(n):
        for j in range(n):
            if i == j:
                similarity_mat[i][j] = 1
            else:
                i_sq = np.sum(filled_mat[i] ** 2)
                j_sq = np.sum(filled_mat[j] ** 2)
                ij_mul = np.sum(filled_mat[i] * filled_mat[j])
                similarity_mat[i][j] = ij_mul / ((i_sq * j_sq) ** 0.5) if i_sq != 0 and j_sq != 0 else 0

    filled_mat[user_match.index(user_id)] = row_conserved

    # 예상 평점 계산
    expected_ratings = np.zeros(m)

    for i in range(m):
        if row_conserved[i] != 0:
            expected_ratings[i] = 0
        else:
            upper = np.sum(similarity_mat[user_match.index(user_id)] * filled_mat[:,i])
            lower = np.sum(similarity_mat[user_match.index(user_id)]) - 1
            if lower == 0:
                expected_ratings[i] = 0
            else:
                expected_ratings[i] = upper / lower

    # 예상 평점이 가장 높은 영화 출력
    max_idx = None
    max_rating = 0
    for i, expected_rating in enumerate(expected_ratings):
        if seen_movies[i] == 0 and max_rating < expected_rating:
            max_rating = expected_rating
            max_idx = i
    if max_idx is None:
        results = [{'id': None, 'title': None, 'director':None, 'price':None, 'avg. rating':None, 'expected rating':None}]
    else:
        cursor.execute("SELECT movie_id AS id, movie_title AS title, movie_director AS director, price, AVG(rating) AS 'avg. rating' "
                       "FROM movies NATURAL JOIN reservations "
                       "WHERE movie_id = %s "
                       "GROUP BY movie_id, movie_title, movie_director, price ", [movie_match[max_idx]])
        results = cursor.fetchall()
        results[0]["expected rating"] = round(expected_ratings[max_idx], 4)
    print_results_in_table(results)

    # YOUR CODE GOES HERE
    pass


# Total of 70 pt.
def main():
    while True:
        print('============================================================')
        print('1. initialize database')
        print('2. print all movies')
        print('3. print all users')
        print('4. insert a new movie')
        print('5. remove a movie')
        print('6. insert a new user')
        print('7. remove an user')
        print('8. book a movie')
        print('9. rate a movie')
        print('10. print all users who booked for a movie')
        print('11. print all movies booked by an user')
        print('12. recommend a movie for a user using popularity-based method')
        print('13. recommend a movie for a user using user-based collaborative filtering')
        print('14. exit')
        print('15. reset database')
        print('============================================================')
        menu = int(input('Select your action: '))

        if menu == 1:
            initialize_database()
        elif menu == 2:
            print_movies()
        elif menu == 3:
            print_users()
        elif menu == 4:
            insert_movie()
        elif menu == 5:
            remove_movie()
        elif menu == 6:
            insert_user()
        elif menu == 7:
            remove_user()
        elif menu == 8:
            book_movie()
        elif menu == 9:
            rate_movie()
        elif menu == 10:
            print_users_for_movie()
        elif menu == 11:
            print_movies_for_user()
        elif menu == 12:
            recommend_popularity()
        elif menu == 13:
            recommend_item_based()
        elif menu == 14:
            print('Bye!')
            break
        elif menu == 15:
            reset()
        else:
            print('Invalid action')


if __name__ == "__main__":
    main()
