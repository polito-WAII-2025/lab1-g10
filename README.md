[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/vlo9idtn)
# lab1-wa2025

## Docker Setup Instructions

This guide explains how to build and run the Docker container for the **RoutAnalyzer** application.

### Building the Docker Image

1. Navigate to the `RoutAnalyzer` directory:
   ```sh
   cd RoutAnalyzer
   ```
2. Build the Docker image using the following command:
   ```sh
   docker build -t lab .
   ```

### Running the Docker Container

1. Move one directory **above** the folder you want to mount into the Docker container.
2. Run the following command, replacing `folderToMount` with the actual folder name. Use the appropriate syntax for your operating system:
    - **Linux & macOS**:
      ```sh
      docker run -v ./folderToMount:/app/files lab
      ```
    - **Windows**:
      ```powershell
      docker run -v .\folderToMount:/app/files lab
      ```
            
### Required Files in `folderToMount`

Inside `folderToMount`, the following files should be present:
- `waypoints.csv`
- `custom-parameters.yml`

If these files are **not** found, the application will use default versions of them.

## Output Files

After execution, a new directory called `output` will be created inside the mounted folder (`folderToMount`). This directory will contain:
- `output.json`
- `output_custom.json`

These files contain the results of the processing performed by the application.

---

Ensure that the necessary files are correctly placed before running the container to get the expected outputs.


