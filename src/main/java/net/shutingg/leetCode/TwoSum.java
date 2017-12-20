package net.shutingg.leetCode;

public class TwoSum {
    public int[] twoSum(int[] nums, int target) {
        if(nums.length <= 1){
            return null;
        }
        int[] res = new int[2];
        for(int i=0; i < nums.length; i++){
            for(int j=i+1; j < nums.length; j++){
                if(nums[i] + nums[j] == target){
                    res[0] = i;
                    res[1] = j;
                    return res;
                }
            }
        }
        return null;
    }
}