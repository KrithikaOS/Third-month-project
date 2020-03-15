function checkEmpty(buttonId) {
    if (document.getElementById('A').value == "" || document.getElementById('B').value == "" || document.getElementById('C').value == "" || document.getElementById('D').value == "" ||
        document.getElementById('description').value == "" ) {
        var errorMsg = "Please fill out the fields";
        document.getElementById("error").innerHTML = errorMsg;
        return false;
    } else {
        let butId = buttonId;
        createQuestionObject(butId);
        //   return true;
        //  document.getElementById('form').submit();
        // alert("Form Submitted Successfully...");
    }
}
//Function To Display Popup
function openAddQuestionForm() {
    document.getElementById('question').style.display = "block";
}
//Function to Hide Popup
function cancelAddQuestionForm() {
    document.getElementById('question').style.display = "none";
}

function createQuestionObject(buttonId) {
    let button = buttonId;
    var questionObj = {
        tag: document.querySelector('input[name=tag]:checked').value,
        description: document.getElementById('description').value,
        option: {
            A: document.getElementById('A').value,
            B: document.getElementById('B').value,
            C: document.getElementById('C').value,
            D: document.getElementById('D').value
        },
        correctAns: document.querySelector('input[name=correctAnswer]:checked').value,
    }
    storeQuestion(JSON.stringify(questionObj), button);
}

function storeQuestion(questionObj, buttonId) {
    var questionList = [];
    let clickedButton = buttonId;
    if (typeof(Storage) !== "undefined") {

        switch (clickedButton) {

            case "add":
                console.log("inside add");
                console.log(clickedButton);
                if (localStorage.clickcount) {
                    console.log("inside click count");
                    if (localStorage.clickcount < 4) {
                        localStorage.clickcount = Number(localStorage.clickcount) + 1;
                        let questionNum = Number(localStorage.clickcount);
                        localStorage.setItem(Number(localStorage.clickcount) - 1, questionObj);
                        console.log(localStorage.clickcount - 1);
                        console.log("Question added");
                        document.getElementById("form").reset();

                        document.getElementById("questionNum").innerHTML = questionNum + 1;

                    } else {
                        alert("you cannot add more than 5 questions. Click Submit to submit the questions");

                    }

                } else {
                    localStorage.clickcount = 1;
                    let questionNum = Number(localStorage.clickcount);
                    localStorage.setItem(Number(localStorage.clickcount) - 1, questionObj);
                    console.log(Number(localStorage.clickcount) - 1);
                    console.log("Question added");
                    document.getElementById("form").reset();
                    document.getElementById("questionNum").innerHTML = questionNum + 1;
                }
                break;
            case "save":
                console.log("inside save");
                   if (localStorage.clickcount) {
                                       localStorage.clickcount = Number(localStorage.clickcount) + 1;
                                   } else {
                                       localStorage.clickcount = 1;
                                   }
                    console.log(localStorage.clickcount);
                    localStorage.setItem(Number(localStorage.clickcount) - 1, questionObj);
                    var myQue = localStorage.getItem(Number(localStorage.clickcount - 1));
                    console.log(myQue);


                for (let itr = 0; itr < Number(localStorage.clickcount); itr++) {
                    var que = localStorage.getItem(itr);
                    questionList.push(que);
                }
                localStorage.clickcount = 0;
                addQuestion(questionList);
                break;
        }

    }

}

function addQuestion(questionList) {
    var promises = [],
        i = 0;
    var url = "http://localhost:8080/api/question/addQuestion";
    questionList.map(que => {
        promises[i++] = makeAjaxCall(url, {
            method: 'POST',
            request: JSON.parse(que),
            async: true
        })
    });

    Promise.all(promises).then((addQuestionResponse) => {
        console.log("Questions sent to db", addQuestionResponse);
        window.open("/QuizHubApplication/html/Dashboard.html")
    }).catch(error => { console.log("error", error); });

}

function processAddQuestionResponse(addQuestionResponse) {
    console.log(addQuestionResponse);
}

function errorHandler(statusCode) {
    console.log("failed with status", status);
}

/*function addQuestion(questionList) {
    var details, statusCode;
    var url = "http://localhost:8080/api/question/addQuestion";
    var response = makeAjaxRequest(url, {
        method: 'POST',
        request: questionList,
        /* request: {
                    tag: document.querySelector('input[name=tag]:checked').value,
                    status: document.querySelector('input[name=status]:checked').value,
                    description: document.getElementById('description').value,
                    option: {
                        A: document.getElementById('A').value,
                        B: document.getElementById('B').value,
                        C: document.getElementById('C').value,
                        D: document.getElementById('D').value
                    },
                    correctAns: document.getElementById('correctAnswer').value 

                }, 
        async: true
    }, function(addedQuestion, statusCode) {
        if (statusCode === 200) {
            alert("Question added successfully");
            var myJSON = JSON.stringify(addedQuestion);
            /*document.getElementById('tag').innerHTML = addedQuestion.data.question.tag;
            document.getElementById('status').innerHTML = addedQuestion.data.question.status;
            document.getElementById('description').innerHTML = addedQuestion.data.question.description;
            document.getElementById('A').innerHTML = addedQuestion.data.question.option.A;
            document.getElementById('B').innerHTML = addedQuestion.data.question.option.B;
            document.getElementById('C').innerHTML = addedQuestion.data.question.option.C;
            document.getElementById('D').innerHTML = addedQuestion.data.question.option.D;
            document.getElementById('correctAnswer').innerHTML = addedQuestion.data.question.correctAns; 
            cancelAddQuestionForm();
        }
        if (statusCode === 401) {
            alert("please login first");
            location.replace("http://localhost:8080/");
        }
    });
} */