function openDisplayQuestions() {
    document.getElementById('DisplayQuestions').style.display = "block";
    getAllQuestions();

}
//Function to Hide Popup
function cancelDisplayQuestions() {
    //  document.getElementById('popupFormDisplayQuestions').reset();
    // localStorage.clickcount = 0;
    // localStorage.clear();
    document.getElementById('DisplayQuestions').style.display = "none";
}


function getAllQuestions() {

    var url = "/api/question/getAllQuestions/";
    makeAjaxCall(url, {
        method: 'GET',
        async: true
    }).then((getAllQuestionsResponse) => {
        var questions = getAllQuestionsResponse.data.questions;
        var currentUserRating = getAllQuestionsResponse.data.currentUserRating;
        console.log(getAllQuestionsResponse);
        displayAllQuestions(questions, currentUserRating);

    }).catch(error => {
        document.getElementById('popupFormDisplayQuestions').innerHTML = "";
        document.getElementById('popupFormDisplayQuestions').innerHTML = "no questions to display";
        console.log("error", error);
    });

}



function displayAllQuestions(questions, currentUserRating) {

    document.getElementById('popupFormDisplayQuestions').innerHTML = "";

    for (let i = 0; i < questions.length; i++) {
        let para = document.createElement("table");
        para.innerHTML = ` <td name="question" value=${questions[i].id}>${i+1}. ${questions[i].description}
              <tr id="option"> A:   ${questions[i].option.A}   B:   ${questions[i].option.B}   C:   ${questions[i].option.C}   D:   ${questions[i].option.D}</td> <br>
              <fieldset class="rating" id="${questions[i].id}"  onclick=updateRating(this.id)>
            <input type="radio" id="star5${questions[i].id}" name="rating${questions[i].id}"  value="5"/>
            <label class="full" for="star5${questions[i].id}" title="Awesome - 5 stars"></label>
            <input type="radio" id="star4${questions[i].id}" name="rating${questions[i].id}" value="4" />
            <label class="full" for="star4${questions[i].id}" title="Pretty good - 4 stars"></label>
            <input type="radio" id="star3${questions[i].id}" name="rating${questions[i].id}" value="3" />
            <label class="full" for="star3${questions[i].id}" title="Good - 3 stars"></label>
            <input type="radio" id="star2${questions[i].id}" name="rating${questions[i].id}" value="2" />
            <label class="full" for="star2${questions[i].id}" title="ok - 2 stars"></label>
            <input type="radio" id="star1${questions[i].id}" name="rating${questions[i].id}" value="1" />
            <label class="full" for="star1${questions[i].id}" title="Very bad - 1 star"></label>
        </fieldset>
           Rating:<tr id= "${questions[i].averageRating}">${questions[i].averageRating}</tr>`
        document.getElementById('popupFormDisplayQuestions').appendChild(para);
    }
    if (currentUserRating !== undefined) {
        let userRating = Object.entries(currentUserRating);
        userRating.map(que => {
            document.getElementById("star" + que[1] + que[0]).checked = true;
        });
    }
}


function updateRating(qId) {
    var rating = document.querySelector("input[name=rating" + qId + "]:checked").value;
    var url = "/api/question/giveRating";
    makeAjaxCall(url, {
        method: 'POST',
        request: {
            questionId: qId,
            rating: rating
        },
        async: true
    }).then((updateRatingResponse) => {
        console.log(updateRatingResponse);

    }).catch(error => { console.log("error", error); });

}