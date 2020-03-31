function openCreatedTests() {
    document.getElementById('DisplayTests').style.display = "block";
    getAllTests();

}
//Function to Hide Popup
function cancelDisplayTests() {
    document.getElementById('DisplayTests').style.display = "none";
}

function getAllTests() {

    const url = "/api/test/getTests";
    makeAjaxCall(url, {
        method: 'GET',
        async: true
    }).then((getAllTestsResponse) => {
        let testDetails = getAllTestsResponse.data.testList;
        displayTests(testDetails);

    }).catch(error => {
        document.getElementById('popupFormDisplayTests').innerHTML = "";
        document.getElementById('popupFormDisplayTests').innerHTML = "No tests created so far.";
        console.log("error", error);
    });

}

function displayTests(testDetails) {
    var tests = '';
    tests = `<tr> <th style="border: 1px solid black;">emailId</th> <th style="border: 1px solid black;">URL</th> <th style="border: 1px solid black;">Status</th> <th style="border: 1px solid black;">Result</th> <tr>`;
    for (var i in testDetails) {
        tests +=
            `<tr>
                                <td style="border: 1px solid black;"> ${testDetails[i].userEmail}</td>
                                <td style="border: 1px solid black;"> ${testDetails[i].testURL}</td>
                                <td style="border: 1px solid black;"> ${testDetails[i].status}</td>
                                <td style="border: 1px solid black;">  ${testDetails[i].result}</td>
                                </tr>`

    }
    document.getElementById('popupFormDisplayTests').innerHTML = tests;

}