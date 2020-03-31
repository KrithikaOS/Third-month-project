function openUserSignUpForm() {
    cancelTestForm();
    document.getElementById('userSignUpForm').style.display = "block";
}

function cancelUserSignUpForm() {
    document.getElementById('userSignUpForm').style.display = "none";
}

function checkEmptyUserSignUpForm() {


    if (document.getElementById('userPassword').value == "" || document.getElementById('firstNameUser').value == "" || document.getElementById('lastNameUser').value == "") {
        var errorMsg = "Please fill out the fields";
        document.getElementById("errorMsg").innerHTML = errorMsg;
        return false;
    } else {
        addUser();

    }

}

function addUser()

{
    var url = "/api/client/userSignup";
    localStorage.setItem("userEmail", document.getElementById('userEmailID').textContent);
    makeAjaxCall(url, {
        method: 'POST',
        request: {
            firstName: document.getElementById('firstNameUser').value,
            lastName: document.getElementById('lastNameUser').value,
            email: document.getElementById('userEmailID').textContent,
            password: document.getElementById('userPassword').value
        },
        async: true
    }).then((signUpResponse) => {
        console.log("sign up successful", signUpResponse);
        window.location.replace("/QuizHubApplication/html/CreateTest.html");
    }).catch(error => { console.log("error", error); });



}

function loginUser() {

    if (document.getElementById('password').value == "") {

        document.getElementById("error").innerHTML = "Please enter a password";
        return false;

    }
    var url = "/api/user/userLogin";

    makeAjaxCall(url, {
        method: 'POST',
        request: {
            email: document.getElementById('email').textContent,
            password: document.getElementById('password').value
        },
        async: true

    }).then((userLoginResponse) => {
        //making ajax call if user login promise is resolved
        location.replace(window.location.href + "/testStart");
        // var currentURL = window.location.href + "/testStart";
        // makeAjaxCall(currentURL, {
        //     method: 'GET',
        //     async: true
        // }).then((startTestResponse) => {
        //     location.replace(window.location.href + "/testStart");
        //     console.log("test start" + startTestResponse);
        // }).catch(error => { console.log("error", error); });


    }).catch(error => {
        document.getElementById("error").innerHTML = "Password incorrect";
        console.log("error", error);
    });

}