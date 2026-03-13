/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */


import {
  AuthenticationControllerApi,
  LoginRequestDTO,
  LoginResponseDTO,
} from "../generated-skeleton-api";

/**
 * Try to log in a user
 * @param login the login data of the user (username and password)
 *
 * @returns Promise with the status and data of the response
 * @throws Error if the request fails
 */
const login = async (loginRequestDTO: LoginRequestDTO): Promise<LoginResponseDTO> => {
  // Send the request, await the response
  const autenticationController = new AuthenticationControllerApi();
  const response = await autenticationController.authenticateUser({
    loginRequestDTO: loginRequestDTO,
  });

  // If the response is not OK (e.g., status not 200), throw an error.
  // The response from axios-based SDK may have status in response.status.
  if (response.status !== 200) {
    throw new Error(`Login failed with status ${response.status}`);
  }

  // Return the response
  return response.data;
};

export const AuthApi = {
  login,
};
