document.getElementById("uploadForm").addEventListener("submit", handleFormSubmit);

function handleFormSubmit(event) {
    event.preventDefault(); // prevent default form submission
    var form = event.target;
    var formData = new FormData(form);

    var xhr = new XMLHttpRequest();
    xhr.open("POST", form.action, true);

    xhr.onload = function () {
        var response = JSON.parse(xhr.responseText);
        var messageDiv = document.getElementById("responseMessage");

        if (xhr.status === 200) {
            if (response.status === "exists") {
                var overwrite = confirm("File already exists. Do you want to overwrite it?");
                if (overwrite) {
                    formData.append("overwrite", "true");
                    xhr.open("POST", form.action, true);
                    xhr.send(formData);
                } else {
                    messageDiv.innerHTML = "<p>File upload cancelled.</p>";
                }
            } else if (response.status === "success") {
                messageDiv.innerHTML = "<p>" + response.message + "</p>";
                setTimeout(function () {
                    updateFileList(); // Refresh the file list
                }, 1000);
            } else if (response.status === "error") {
                messageDiv.className = "error-message";
                messageDiv.innerHTML = "<p>" + response.message + "</p>";
            }
        } else {
            messageDiv.className = "error-message";
            messageDiv.innerHTML = "<p>Upload failed with status: " + xhr.status + "</p>";
        }

        setTimeout(function () {
            updateFileList();
        }, 2000);
    };

    xhr.onerror = function () {
        document.getElementById("responseMessage").innerHTML = "<p class='error-message'>Upload failed.</p>";
    };

    xhr.upload.onprogress = function (event) {
        if (event.lengthComputable) {
            var percentComplete = (event.loaded / event.total) * 100;
            console.log("Upload progress: " + percentComplete + "%");
            document.getElementById("progressBar").style.width = percentComplete + "%";
            document.getElementById("progressText").innerText = Math.round(percentComplete) + "% uploaded";
        }
    };

    xhr.send(formData);
}

function updateFileList() {
    // Function to update the list of uploaded files
    // This will be specific to your application
}
