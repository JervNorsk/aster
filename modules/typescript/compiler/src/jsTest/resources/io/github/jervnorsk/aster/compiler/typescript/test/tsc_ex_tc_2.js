class A {
    constructor() {
        console.log("Building Class A")
    }
}

function B() {}

B.prototype.fn_1 = function () {
    console.log("fn_1()")
    return this
}

B.prototype.fn_2 = function (a, b) {
    console.log("fn_2()")
    return a + b
}
