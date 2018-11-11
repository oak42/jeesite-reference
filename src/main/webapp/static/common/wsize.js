/*!
 * Copyright &copy; 2012-2016 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 * 
 * 主框架窗口大小调整
 * @author ThinkGem
 * @version 2013-11-09
 */

$("#left").width(leftWidth);
$("#openClose").click(function () {
    if ($(this).hasClass("close")) {
        $(this).removeClass("close");
        $(this).addClass("open");
        $("#left").animate({width: 0, opacity: "hide"});
        $("#right").animate({width: $("#content").width() - $("#openClose").width() - 5}, function () {
            if (typeof openCloseClickCallBack == 'function') {
                openCloseClickCallBack(true);
            }
            wSize();
        });
    } else {
        $(this).addClass("close");
        $(this).removeClass("open");
        $("#left").animate({width: leftWidth, opacity: "show"});
        $("#right").animate({width: $("#content").width() - $("#openClose").width() - leftWidth - 9}, function () {
            if (typeof openCloseClickCallBack == 'function') {
                openCloseClickCallBack(true);
            }
            wSize();
        });
    }
});
if (!Array.prototype.map)
    Array.prototype.map = function (fn, scope) {
        var result = [], ri = 0;
        for (var i = 0, n = this.length; i < n; i++) {
            if (i in this) {										//此this是指此map function被call的piggy-backing的array喽？
                result[ri++] = fn.call(scope, this[i], i, this);		//function的call、apply，首个形参都是function的this value...其余为正常一般形参...
            }
        }
        return result;
    };
var getWindowSize = function () {
    return ["Height", "Width"].map(function (name) {
        return window["inner" + name] ||
            document.compatMode === "CSS1Compat" && document.documentElement["client" + name] || document.body["client" + name];
    });
};
$(window).resize(function () {
    wSize();
});
wSize(); // 在主窗体中定义，设置调整目标