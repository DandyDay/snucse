import java.util.StringTokenizer;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;

/*
   1. 아래와 같은 명령어를 입력하면 컴파일이 이루어져야 하며, Solution4 라는 이름의 클래스가 생성되어야 채점이 이루어집니다.
       javac Solution4.java -encoding UTF8


   2. 컴파일 후 아래와 같은 명령어를 입력했을 때 여러분의 프로그램이 정상적으로 출력파일 output4.txt 를 생성시켜야 채점이 이루어집니다.
       java Solution4

   - 제출하시는 소스코드의 인코딩이 UTF8 이어야 함에 유의 바랍니다.
   - 수행시간 측정을 위해 다음과 같이 time 명령어를 사용할 수 있습니다.
       time java Solution4
   - 일정 시간 초과시 프로그램을 강제 종료 시키기 위해 다음과 같이 timeout 명령어를 사용할 수 있습니다.
       timeout 0.5 java Solution4   // 0.5초 수행
       timeout 1 java Solution4     // 1초 수행
 */

class Solution4 {
	static final int max_n = 1000000;

	static int n;
	static int[][] A = new int[3][max_n];
	static int Answer;

	static int[][] dp = new int[6][max_n];

	/*
		pattern0 : ○, △, ×
		pattern1 : ○, ×, △
		pattern2 : △, ×, ○
		pattern3 : △, ○, ×
		pattern4 : ×, ○, △
		pattern5 : ×, △, ○
		0, 2, 4 compatible
		1, 3, 5 compatible
	 */
	static int[][] compatible = new int[][]{{2, 4}, {3, 5}, {0, 4}, {1, 5}, {0, 2}, {1, 3}};

	public static int getColumn(int n, int pattern)
	{
		if (pattern == 0)
			return A[0][n] - A[2][n];
		else if (pattern == 1)
			return A[0][n] - A[1][n];
		else if (pattern == 2)
			return A[2][n] - A[1][n];
		else if (pattern == 3)
			return A[1][n] - A[2][n];
		else if (pattern == 4)
			return A[1][n] - A[0][n];
		else if (pattern == 5)
			return A[2][n] - A[0][n];
		return 0;		//can't be reached
	}

	public static void updateColumns(int n) {
		for (int pattern = 0; pattern < 6; pattern++) {
			dp[pattern][0] = getColumn(0, pattern);
		}
		for (int k = 1; k < n; k++) {
			for (int pattern = 0; pattern < 6; pattern++) {
				dp[pattern][k] = dp[compatible[pattern][0]][k-1] + getColumn(k, pattern);
				for (int comp = 0; comp < 2; comp++) {
					if (dp[pattern][k] < dp[compatible[pattern][comp]][k-1] + getColumn(k, pattern))
						dp[pattern][k] = dp[compatible[pattern][comp]][k-1] + getColumn(k, pattern);
				}
			}
		}
	}

	public static int getAnswer(int n)
	{
		updateColumns(n);

		int max = dp[0][n - 1];
		for (int i = 1; i < 6; i++)
			if (max < dp[i][n - 1])
				max = dp[i][n - 1];
		return max;
	}

	public static void main(String[] args) throws Exception {
		/*
		   동일 폴더 내의 input4.txt 로부터 데이터를 읽어옵니다.
		   또한 동일 폴더 내의 output4.txt 로 정답을 출력합니다.
		 */
		BufferedReader br = new BufferedReader(new FileReader("input4.txt"));
		StringTokenizer stk;
		PrintWriter pw = new PrintWriter("output4.txt");

		/*
		   10개의 테스트 케이스가 주어지므로, 각각을 처리합니다.
		 */
		for (int test_case = 1; test_case <= 10; test_case++) {
			/*
			   각 테스트 케이스를 표준 입력에서 읽어옵니다.
			   먼저 놀이판의 열의 개수를 n에 읽어들입니다.
			   그리고 첫 번째 행에 쓰여진 n개의 숫자를 차례로 A[0][0], A[0][1], ... , A[0][n-1]에 읽어들입니다.
			   마찬가지로 두 번째 행에 쓰여진 n개의 숫자를 차례로 A[1][0], A[1][1], ... , A[1][n-1]에 읽어들이고,
			   세 번째 행에 쓰여진 n개의 숫자를 차례로 A[2][0], A[2][1], ... , A[2][n-1]에 읽어들입니다.
			 */
			stk = new StringTokenizer(br.readLine());
			n = Integer.parseInt(stk.nextToken());
			for (int i = 0; i < 3; i++) {
				stk = new StringTokenizer(br.readLine());
				for (int j = 0; j < n; j++) {
					A[i][j] = Integer.parseInt(stk.nextToken());
				}
			}


			/////////////////////////////////////////////////////////////////////////////////////////////
			/*
			   이 부분에서 여러분의 알고리즘이 수행됩니다.
			   문제의 답을 계산하여 그 값을 Answer에 저장하는 것을 가정하였습니다.
			 */
			/////////////////////////////////////////////////////////////////////////////////////////////
			Answer = getAnswer(n);


			// output4.txt로 답안을 출력합니다.
			pw.println("#" + test_case + " " + Answer);
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

