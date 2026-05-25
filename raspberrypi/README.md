# RaspberryPi Config

- Credentials for the pi:
    * username: 'pi' 
    * password: 'password'
- Auth credentials for the webapp:
    * username: 'raspberry-pi'
    * password: 'passwrd'


## General Setup Overview 

- Static information is configured in `conf.yaml` this includes the server url from the webapp and authentication credentials. This is the only thing that need manual configuration in the entire setup process. An example is given below

```yaml
webapp:
  server_url: "http://100.95.135.11:8080"
  local_listen_port: 8080

auth:
  username: "raspberry-pi"
  password: "passwrd"
```
- The workflow is as follows
    * docker container starts main application
    * this waits for the `./configure` script to be executed, which seeds the database from the configs in `conf.yaml`
    * after that the pi tries to get the token needed for authentication using the credentials from `conf.yaml`
    * after that succeeds, the pi waits for the initial config from the webapp, which is sent when sysadmin configures it on the website
    * then, pi starts normal work, dynamically handling config changes from the webapp (like adding/deleting sensors) 
- after general setup, the pi receives all configs from the webapp dynamically via a REST server

## General Setup Execution

- Make sure `ssh` works without password so the deploy script works without password. If you don't want to set this up simply enter the password above when prompted during setup.
    * First generate a key on your machine with `ssh-keygen -t ed25519`
    * Copy the key to the raspberrypi with `ssh-copy-id pi@<pi_ip>`, enter the password when prompted 
- Set the raspberrypi ip address by either modifying `deploy.sh` directly or typing `PI_HOST=<pi_ip>` in the terminal
- Now there are two ways to set up the services on the pi:
    1. Set up everything on your machine:
        * Modify `conf.yaml` with the appropriate values
        * Start the `deploy.sh` script with the parameter `start` like this: `./deploy.sh start`
        * This will set up docker services on the pi and start everything, no need to manually `ssh` into the pi 
    2. Manual setup on the pi:
        * Run `deploy.sh` without parameters like `./deploy.sh`
        * This will copy all necessary files to the pi and exit 
        * Needs manual activation, so `ssh` into the pi, start the docker container  with `docker compose up --build -d` and complete the setup by running  the `./configure` script (after the docker container started).
        * Note that `conf.yaml` can either be edited on your machine before running `deploy.sh` or remotely on the pi (before running `./configure`)
- Now everything should be up and running, you can view the logs on the pi either directly from the log file or by running `docker logs -f pi-gateway-1` to see them in real time
- **WARNING:** Running `./deploy.sh` wipes everything and starts fresh, including logs and database 
