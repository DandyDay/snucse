import java.util.StringTokenizer;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;

/*
   1. 아래와 같은 명령어를 입력하면 컴파일이 이루어져야 하며, Solution1 라는 이름의 클래스가 생성되어야 채점이 이루어집니다.
       javac Solution1.java -encoding UTF8

   2. 컴파일 후 아래와 같은 명령어를 입력했을 때 여러분의 프로그램이 정상적으로 출력파일 output1.txt 를 생성시켜야 채점이 이루어집니다.
       java Solution1

   - 제출하시는 소스코드의 인코딩이 UTF8 이어야 함에 유의 바랍니다.
   - 수행시간 측정을 위해 다음과 같이 time 명령어를 사용할 수 있습니다.
       time java Solution1
   - 일정 시간 초과시 프로그램을 강제 종료 시키기 위해 다음과 같이 timeout 명령어를 사용할 수 있습니다.
       timeout 0.5 java Solution1   // 0.5초 수행
       timeout 1 java Solution1     // 1초 수행
 */

class Solution1 {

    static final int MAX_N = 1000;
	static final int MAX_E = 100000;
	static final int Div = 100000000;  // 1억
	static int N, E;
	static int[] U = new int[MAX_E], V = new int[MAX_E], W = new int[MAX_E];
	static int[] Answer1 = new int[MAX_N+1];
	static int[] Answer2 = new int[MAX_N+1];

	static int[] prevCheck = new int[MAX_N + 1];
    static double start1, start2;
    static double time1, time2;


	private static int[] BellmanFord1(int start) {
		 for (int i = 1; i <= N; i++)
			 Answer1[i] = Div;
		 Answer1[1] = 0;
		 for (int i = 1; i <= N; i++) {
			 for (int e = 0; e < E; e++) {
				 if (Answer1[U[e]] + W[e] < Answer1[V[e]])
					 Answer1[V[e]] = Answer1[U[e]] + W[e];
			 }
		 }
		 return Answer1;
	}

	private static int[] BellmanFord2(int start) {
		for (int i = 1; i <= N; i++) {
			Answer2[i] = Div;
			prevCheck[i] = 0;
		}

		Answer2[1] = 0;
		for (int i = 1; i <= N; i++) {
			for (int e = 0; e < E; e++) {
				if (prevCheck[U[e]] >= i - 1 && Answer2[U[e]] + W[e] < Answer2[V[e]]) {
					Answer2[V[e]] = Answer2[U[e]] + W[e];
					prevCheck[V[e]] = i;
				}
			}
		}
		return Answer2;
	}

	public static void main(String[] args) throws Exception {
		/*
		   동일 폴더 내의 input1.txt 로부터 데이터를 읽어옵니다.
		   또한 동일 폴더 내의 output1.txt 로 정답을 출력합니다.
		 */
		BufferedReader br = new BufferedReader(new FileReader("input1.txt"));
		StringTokenizer stk;
		PrintWriter pw = new PrintWriter("output1.txt");

		/*
		   10개의 테스트 케이스가 주어지므로, 각각을 처리합니다.
		 */
		for (int test_case = 1; test_case <= 10; test_case++) {
			/*
			   각 테스트 케이스를 표준 입력에서 읽어옵니다.
			   먼저 정점의 개수와 간선의 개수를 각각 N, E에 읽어들입니다.
			   그리고 각 i번째 간선의 시작점의 번호를 U[i], 끝점의 번호를 V[i]에, 간선의 가중치를 W[i]에 읽어들입니다.
			   (0 ≤ i ≤ E-1, 1 ≤ U[i] ≤ N, 1 ≤ V[i] ≤ N)
			 */
			stk = new StringTokenizer(br.readLine());
			N = Integer.parseInt(stk.nextToken()); E = Integer.parseInt(stk.nextToken());
			stk = new StringTokenizer(br.readLine());
			for (int i = 0; i < E; i++) {
				U[i] = Integer.parseInt(stk.nextToken());
				V[i] = Integer.parseInt(stk.nextToken());
				W[i] = Integer.parseInt(stk.nextToken());
			}

            /* Problem 1-1 */
            start1 = System.currentTimeMillis();
			Answer1 = BellmanFord1(1);
            time1 = (System.currentTimeMillis() - start1);

            /* Problem 1-2 */
            start2 = System.currentTimeMillis();
            Answer2 = BellmanFord2(1);
            time2 = (System.currentTimeMillis() - start2);

            // output1.txt로 답안을 출력합니다.
			pw.println("#" + test_case);
            for (int i = 1; i <= N; i++) {
                pw.print(Answer1[i]);
                if (i != N)
                    pw.print(" ");
                else
                    pw.print("\n");
            }
            pw.println(time1);

            for (int i = 1; i <= N; i++) {
                pw.print(Answer2[i]);
                if (i != N)
                    pw.print(" ");
                else
                    pw.print("\n");
            }
            pw.println(time2);
			/*
			   아래 코드를 수행하지 않으면 여러분의 프로그램이 제한 시간 초과로 강제 종료 되었을 때,
			   출력한 내용이 실제로 파일에 기록되지 않을 수 있습니다.
			   따라서 안전을 위해 반드시 flush() 를 수행하시기 바랍니다.
			 */
			pw.flush();
		}

		br.close();
		pw.close();
	}
}













