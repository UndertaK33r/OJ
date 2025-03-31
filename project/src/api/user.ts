import {
  UserControllerService,
  UserLoginRequest,
  UserRegisterRequest,
  UserUpdateMyRequest,
} from "../../generated";

/**
 * 获取当前登录用户
 */
export const getLoginUser = async () => {
  return UserControllerService.getLoginUserUsingGet();
};

/**
 * 获取用户做题历史
 */
export const getUserQuestionHistory = async () => {
  return UserControllerService.getUserQuestionHistoryUsingGet();
};

/**
 * 更新用户信息
 */
export const updateUserInfo = async (
  userUpdateMyRequest: UserUpdateMyRequest
) => {
  return UserControllerService.updateMyUserUsingPost(userUpdateMyRequest);
};
