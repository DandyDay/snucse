//import java.util.*;
//
//public class Test {
//    public static void main(String[] args) {
//
//        final int CAPACITY_mini = 16;
//
//        int[] arr1 = new int[CAPACITY_mini];
//
//        Random random1 = new Random();
//        int rMin_mini = 1;
//        int rMax_mini = 999;
//
//        for (int i = 0; i < arr1.length; i++)
//            arr1[i] = random1.nextInt(rMax_mini - rMin_mini + 1) + rMin_mini;
//
//        printArrBySpace(arr1, "arr1");
//
//        int[] arr2 = SortingTest.DoQuickSort(arr1);
//
//        System.out.printf("\n");
//        printArrBySpace(arr2, "arr2");
//        System.out.println();
//
//
//        final boolean doTest = true;
//        final char[] sortingOrder = {'B', 'I', 'H', 'M', 'Q', 'R'};
//        final boolean[] doEachSorting = {true, true, true, true, true, false};
//        final int sortingNum = 6;
//
//        final int[] testNums = {16, 128, 1024};
//        final int[] CAPACITYs = {16, 1024, 65536, 1048567};     // 65536 = 2^16, 1048576 = 2^20
//
//        final int testNum = testNums[0];                        // should bigger than (edge + 1)
//        final int CAPACITY = CAPACITYs[2];
//        final int rMin = -2147483648 / 2 + 1;       // 2147483648 = 2^31
//        final int rMax = 2147483647 / 2;
//
//        final boolean printTestInfo = true;
//        final boolean executeSorting = true;
//        final boolean checkSortedArray = true;
//        final boolean printTestResult = true;
//
//        if (doTest) {
//            System.out.println();
//            System.out.println("================================");
//
//            long[][] runTimes = new long[sortingNum][testNum];
//
//            // print test info
//            if (printTestInfo) {
//                System.out.println("--test start");
//                System.out.printf("testNum = %d, CAPACITY = %d, range : %d ~ %d\n", testNum, CAPACITY, rMin, rMax);
//                for (int i = 0; i < sortingNum; i++) {
//                    System.out.printf("%c : %s,\t", sortingOrder[i], String.valueOf(doEachSorting[i]));
//                }
//                System.out.println();
//                System.out.println();
//            }
//
//            // test for each testCases
//            for (int t = 0; t < testNum; t++) {
//                System.out.printf("now sorting %dth test...\n", t);
//
//                Random random2 = new Random();
//                int[] arr0 = new int[CAPACITY];
//                int[] arrcp = new int[CAPACITY];
//                for (int i = 0; i < arr0.length; i++)
//                    arr0[i] = random2.nextInt(rMax - rMin + 1) + rMin;
//
//                int[] arrB = new int[0];
//                int[] arrI = new int[0];
//                int[] arrH = new int[0];
//                int[] arrM = new int[0];
//                int[] arrQ = new int[0];
//                int[] arrR = new int[0];
//
//                // execute sorting
//                if (executeSorting) {
//                    long currTime;
//                    long prevTime;
//
//                    if (doEachSorting[0]) {
//                        currTime = System.currentTimeMillis();
//                        System.arraycopy(arr0, 0, arrcp, 0, arr0.length);
//                        arrB = SortingTest.DoBubbleSort(arrcp);
//                        prevTime = currTime;
//                        currTime = System.currentTimeMillis();
//                        runTimes[0][t] = currTime - prevTime;
//                    }
//
//                    if (doEachSorting[1]) {
//                        currTime = System.currentTimeMillis();
//                        System.arraycopy(arr0, 0, arrcp, 0, arr0.length);
//                        arrI = SortingTest.DoInsertionSort(arrcp);
//                        prevTime = currTime;
//                        currTime = System.currentTimeMillis();
//                        runTimes[1][t] = currTime - prevTime;
//                    }
//
//                    if (doEachSorting[2]) {
//                        currTime = System.currentTimeMillis();
//                        System.arraycopy(arr0, 0, arrcp, 0, arr0.length);
//                        arrH = SortingTest.DoHeapSort(arrcp);
//                        prevTime = currTime;
//                        currTime = System.currentTimeMillis();
//                        runTimes[2][t] = currTime - prevTime;
//                    }
//
//                    if (doEachSorting[3]) {
//                        currTime = System.currentTimeMillis();
//                        System.arraycopy(arr0, 0, arrcp, 0, arr0.length);
//                        arrM = SortingTest.DoMergeSort(arrcp);
//                        prevTime = currTime;
//                        currTime = System.currentTimeMillis();
//                        runTimes[3][t] = currTime - prevTime;
//                    }
//
//                    if (doEachSorting[4]) {
//                        currTime = System.currentTimeMillis();
//                        System.arraycopy(arr0, 0, arrcp, 0, arr0.length);
//                        arrQ = SortingTest.DoQuickSort(arrcp);
//                        prevTime = currTime;
//                        currTime = System.currentTimeMillis();
//                        runTimes[4][t] = currTime - prevTime;
//                    }
//
//                    if (doEachSorting[5]) {
//                        currTime = System.currentTimeMillis();
//                        System.arraycopy(arr0, 0, arrcp, 0, arr0.length);
//                        arrR = SortingTest.DoRadixSort(arrcp);
//                        prevTime = currTime;
//                        currTime = System.currentTimeMillis();
//                        runTimes[5][t] = currTime - prevTime;
//                    }
//                }           // end if(executeSorting)
//
//                // check the sorted array, if arr is not sorted well, return main()
//                if (checkSortedArray) {
//                    ArrayList<int[]> sortedArrays = new ArrayList<>();
//                    sortedArrays.add(arrB);
//                    sortedArrays.add(arrI);
//                    sortedArrays.add(arrH);
//                    sortedArrays.add(arrM);
//                    sortedArrays.add(arrQ);
//                    sortedArrays.add(arrR);
//
//                    int errIdx = -1;
//                    try {
//                        for (int i = 0; i < sortingNum; i++) {
//                            if (doEachSorting[i]) {
//                                if (!checkSortingIsCorrect(sortedArrays.get(i))) {
//                                    errIdx = i;
//                                    throw new MyException(String.format("error %c", sortingOrder[i]));
//                                }
//                            }
//                        }
//
//                    } catch (MyException e) {
//                        System.out.println("@@error");
//                        System.out.println();
//                        System.out.printf("@%dth testcase\n", t);
//                        System.out.println();
//                        System.out.println("origin array : ");
//                        printArrBySpace(arr0, "arr0");
//                        System.out.println();
//                        System.out.println("sorted array : ");
//                        printArrBySpace(sortedArrays.get(errIdx), "arr" + sortingOrder[errIdx]);
//                        e.printStackTrace();
//                        return;
//                    }
//                }       // end if(checkSortedArray)
//
//                System.out.printf("%dth test done\n", t);
//
//            }   // end for(t), each testCases
//
//            // if for(t) end without exception, all sorting is completed
//            if (checkSortedArray) System.out.println("@@success");
//
//            // print test result
//            if (printTestResult) {
//                System.out.println();
//                System.out.println("--test end");
//                System.out.printf("testNum = %d, CAPACITY = %d, range : %d ~ %d\n", testNum, CAPACITY, rMin, rMax);
//                for (int i = 0; i < sortingNum; i++) {
//                    System.out.printf("%c : %s,\t", sortingOrder[i], String.valueOf(doEachSorting[i]));
//                }
//                System.out.println();
//                System.out.println();
//
//                int runtimesNum = 16;
//                for (int i = 0; i < sortingNum; i++) {
//                    if (doEachSorting[i]) {
//                        System.out.printf("%c sort runTimes : ", sortingOrder[i]);
//                        for (int j = 0; j < Math.min(testNum, runtimesNum); j++) {
//                            System.out.printf("%d, ", runTimes[i][j]);
//                        }
//                        if (testNum > runtimesNum)
//                            System.out.println("...");
//                        else
//                            System.out.println();
//                    }
//                }
//                System.out.println();
//
//                System.out.println("-avg runTime");
//                int edge = 3;
//                long[] avgRunTime = new long[sortingNum];
//                for (int i = 0; i < sortingNum; i++)
//                    if (doEachSorting[i])
//                        for (int j = edge; j < testNum; j++)
//                            avgRunTime[i] += runTimes[i][j];
//                for (int i = 0; i < sortingNum; i++)
//                    avgRunTime[i] /= (testNum - edge);
//                for (int i = 0; i < sortingNum; i++)
//                    if (doEachSorting[i])
//                        System.out.printf("%c sort : %d[ms]\n", sortingOrder[i], avgRunTime[i]);
//                System.out.println();
//            }
//
//            System.out.println("================================");
//        }
//
//    }
//
//    private static boolean checkSortingIsCorrect(int[] sortedArr) throws MyException {
//
//        if (sortedArr == null) {
//            throw new MyException("sortedArr is null");
//        }
//        if (sortedArr.length == 0) {
//            throw new MyException("sortedArr.length is 0");
//        }
//
//        for (int i = 0; i < sortedArr.length - 1; i++) {
//            if (sortedArr[i] > sortedArr[i + 1]) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    public static void printArrBySpace(int[] arr, String arrName) {
//        SortingTest.printArrBySpace(arr, arrName);
//    }
//
//    private static class MyException extends Exception {
//        MyException(String str) {
//            super(str);
//        }
//    }
//
//
//}
//
///*
//================================
//--test start
//testNum = 128, CAPACITY = 65536, range : -1073741823 ~ 1073741823
//B : true,	I : true,	H : true,	M : true,	Q : true,	R : false,
//
//now sorting 0th test...
//0th test done
//now sorting 1th test...
//1th test done
//now sorting 2th test...
//...
//126th test done
//now sorting 127th test...
//127th test done
//@@success
//
//--test end
//testNum = 128, CAPACITY = 65536, range : -1073741823 ~ 1073741823
//B : true,	I : true,	H : true,	M : true,	Q : true,	R : false,
//
//B sort runTimes : 5271, 5180, 5086, 5160, 5170, 5205, 5130, 5176, 5159, 5100, 5165, 5251, 5340, 5275, 5105, 5158, ...
//I sort runTimes : 190, 170, 170, 280, 265, 260, 255, 269, 260, 255, 252, 259, 295, 255, 283, 257, ...
//H sort runTimes : 5, 5, 9, 5, 5, 5, 3, 7, 1, 5, 10, 10, 5, 3, 0, 5, ...
//M sort runTimes : 15, 10, 5, 5, 10, 10, 8, 0, 9, 5, 0, 20, 7, 7, 12, 5, ...
//Q sort runTimes : 5, 5, 5, 5, 0, 5, 9, 9, 5, 5, 8, 4, 0, 5, 5, 5, ...
//
//-avg runTime
//B sort : 5197[ms]
//I sort : 267[ms]
//H sort : 5[ms]
//M sort : 6[ms]
//Q sort : 3[ms]
//
//================================
//
//종료 코드 0(으)로 완료된 프로세스
//
//
//================================
//
//================================
//--test start
//testNum = 128, CAPACITY = 65536, range : -1073741823 ~ 1073741823
//B : false,	I : false,	H : true,	M : true,	Q : true,	R : false,
//
//now sorting 0th test...
//0th test done
//now sorting 1th test...
//1th test done
//now sorting 2th test...
//...
//126th test done
//now sorting 127th test...
//127th test done
//@@success
//
//--test end
//testNum = 128, CAPACITY = 65536, range : -1073741823 ~ 1073741823
//B : false,	I : false,	H : true,	M : true,	Q : true,	R : false,
//
//H sort runTimes : 9, 5, 9, 6, 10, 3, 6, 8, 10, 7, 5, 0, 10, 12, 10, 7, ...
//M sort runTimes : 10, 5, 10, 0, 0, 7, 10, 3, 10, 0, 10, 10, 10, 8, 10, 10, ...
//Q sort runTimes : 10, 11, 5, 9, 10, 5, 5, 9, 3, 10, 5, 0, 0, 0, 3, 0, ...
//
//-avg runTime
//H sort : 5[ms]
//M sort : 6[ms]
//Q sort : 4[ms]
//
//================================
//
// */