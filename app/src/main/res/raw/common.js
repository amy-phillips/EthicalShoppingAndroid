var FUZZY_MATCH_THRESHOLD=0.9;
const BAD_COLOUR='#FF8686';
const AVG_COLOUR='#FFCE79';
const GOOD_COLOUR='#b5ffb6';
const DEBUG_COLOUR='#FFFF00';

// set to 
// 0 for no extra debug
// 1 to colour the background of every product considered
// 2 for more details about why a product doesn't match
const DEBUGGING=1;

function get_best_match(munged_tables, raw_product_name) {
    // strip off (2 pint)
    var product_name = raw_product_name.replace(/\s*(?:\(?\d+ ?pint\)?$)/gmi, "");
    // strip off (Sugar levy applied)
    product_name = product_name.replace(/\s*(?:\(Sugar levy applied\)$)/gmi, "");
    // strip off 400g or 4x180ml or 3kg or x48
    product_name = product_name.replace(/(?:\d+\s*x)*(?:(?:\d+ml$)|(?:(?:\d+\.)?\d+l$)|(?:\d+g$)|(?:(?:\d+\.)?\d+kg$)|(?:x\d+$))/gmi, "");
    product_name=product_name.trim();

    var preprocessed_product_name=pre_process(product_name);
    var best_match=null;
    for(let col_tbl of munged_tables) {
        for(let bb of col_tbl.table) {
            var matchiness = get_matchiness(preprocessed_product_name, bb.preprocessed_title);
            if(best_match==null || matchiness > best_match.matchiness) {
                best_match={};
                best_match.matchiness=matchiness;
                best_match.bb=bb;
                best_match.colour=col_tbl.colour;
                best_match.product_name=product_name; // for debugging
            }
        }
    }

    return best_match;
}

function get_munged_tables(response) {
    munged_tables=[]

    for (let prod_type in response.scores) {
        if (!response.scores.hasOwnProperty(prod_type)) {
            continue;
        }

        munged_tables.push({colour:BAD_COLOUR, table:response.scores[prod_type].table.bad});
        munged_tables.push({colour:AVG_COLOUR, table:response.scores[prod_type].table.average});
        munged_tables.push({colour:GOOD_COLOUR, table:response.scores[prod_type].table.good});
    }

    return munged_tables;
}

function display_call_to_login_if_necessary(response) {
    subscribe_fors=[];
    subscribe_link=null;
    if(response.subscription) {
        subscribe_link=response.subscription;
        // grab the titles of all the foods with an empty recommendations table (these will be the ones you gain if you subscribe)
        for (let value of Object.values(response.scores)) {
            if(value.table.good.length==0 && value.table.average.length==0 && value.table.bad.length==0) {
                if(value.title) {
                    subscribe_fors.push(value.title);
                }
            }
        }
    }
    
    const esCallToLogin = document.querySelector("#es-call_to_login");

    if(subscribe_link && !esCallToLogin) {
        // add a button to link to ethical consumer site for login/subscribe
        var top_bar=document.createElement('table');
        top_bar.className="es-login_or_subscribe-block";
        top_bar.id='es-call_to_login';

        var row = top_bar.insertRow(0);

        var cell1 = row.insertCell(0);
        cell1.setAttribute("width", "40px");
        //cell1.setAttribute("border", "0 !important");

        var imglink = document.createElement('a');
        imglink.href = "https://www.ethicalconsumer.org";
        imglink.setAttribute('target','_blank');

        var img=document.createElement('img');
        img.setAttribute("src", chrome.extension.getURL("images/icon32.png"));
        img.setAttribute("alt", "Ethical Shopping Helper Logo");
        img.className = "es-img-32"
        imglink.appendChild(img);

        cell1.appendChild(imglink);
        cell1.className = "es-login_or_subscribe-td";

        var cell2 = row.insertCell(1);
        cell2.className = "es-login_or_subscribe-td";
        cell2.setAttribute("width", "100%"); // this pushes cell3 to the right of the screen
        //cell2.setAttribute("border","0 !important");

        var linky_p = document.createElement('p');
        var last=subscribe_fors.pop();
        var subscr_fors_str=subscribe_fors.join(", ")+" and "+last;
        linky_p.innerHTML = "Please <a href=\"https://www.ethicalconsumer.org\" target=\"_blank\">sign in</a> or <a href=\""+subscribe_link+"\" target=\"_blank\">subscribe</a> to Ethical Consumer so I can highlight more products for you from categories such as "+subscr_fors_str;
        linky_p.className = "es-login_or_subscribe-p";
        cell2.appendChild(linky_p);

        var cell3 = row.insertCell(2);
        cell3.className = "es-login_or_subscribe-td";
        cell3.setAttribute("width", "100px");
        //cell3.setAttribute("border","0 !important");
        var goaway_linky = document.createElement('p');
        goaway_linky.innerHTML = "<a href=\"javascript:void(0);\">Login Later</a>";
        goaway_linky.onclick = go_away;
        goaway_linky.className = "es-login_or_subscribe-p";
        cell3.appendChild(goaway_linky);

        get_header_location().append(top_bar); // put it on end of body, but css positions it at top of page
    } else if(subscribe_link==null && esCallToLogin) {
        // remove the prompt button - they logged in!
        esCallToLogin.parentNode.removeChild(esCallToLogin);
    }
}

function apply_colour(product_div,colour_div,css_class,short_text,best_match) {
    console.log("apply_colour: "+best_match)
    colour_div.style.backgroundColor = best_match.colour;

    // add a button to link to ethical consumer site for moar infos
    var link = document.createElement('a');
    var table = document.createElement('table');
    table.style.backgroundColor=best_match.colour;
    table.style.marginLeft='auto';
    table.style.marginRight='auto'; // centre the table
    var row = table.insertRow(0);
    var cell1 = row.insertCell(0);
    var cell2 = row.insertCell(1);
    cell1.style.borderStyle='hidden';
    cell1.style.textAlign='right';
    cell2.style.borderStyle='hidden';
    cell2.style.textAlign='left';
    /*
    var img=document.createElement('img');
    img.setAttribute("src", "https://github.com/amy-phillips/EthicalShopping/blob/master/images/icon32.png");
    img.setAttribute("alt", "Ethical Shopping Helper Logo");
    if(short_text) { // no link class means we have more space so can be more verbose
        img.className="es-img-16";
    } else {
        img.className="es-img-32";
    }
    cell1.appendChild(img);
    */
    var linkText = document.createTextNode("("+best_match.bb.title+")");
    if(!short_text) { // more space so can be more verbose
        linkText = document.createTextNode("More details ("+best_match.bb.title+") at "+best_match.bb.link);
    }
    if(DEBUGGING>=2) {
        linkText = document.createTextNode("More details ("+best_match.bb.title+") ("+best_match.product_name+") ("+best_match.matchiness+") at "+best_match.bb.link);
    } 
    cell2.appendChild(linkText);
    link.appendChild(table);
    link.title = "For more details click here to go to the ethical consumer website";
    link.href = best_match.bb.link;
    link.setAttribute('target','_blank');
    link.addEventListener('click', (e) => { e.stopPropagation(); }, false);
    if(css_class) {
        link.className=css_class;
    }
    link.id='es-moar-infos';
    product_div.after(link); //product_div.parentNode.appendChild(link);
}

function colour_product(munged_tables,product_div,colour_div,css_class,short_text,raw_product_name) {
    console.log("colour_product: "+raw_product_name)
    // is this a best buy?
    var best_match=get_best_match(munged_tables, raw_product_name);
    if(best_match!=null && best_match.matchiness>FUZZY_MATCH_THRESHOLD) {
        apply_colour(product_div,colour_div,css_class,short_text,best_match);
    } else if(DEBUGGING>=1) {
        best_match.colour=DEBUG_COLOUR;
        apply_colour(product_div,colour_div,css_class,short_text,best_match);
    }
}   


function go_away() {
/*
    chrome.runtime.sendMessage({command: "go_away"}, function(response) {
        if(response.error) {
            console.log(response.error);
            console.log(response.data);
            return;
        }

        console.log(response);
        display_call_to_login_if_necessary(response);

        colour_page(response);
        return;

      });
*/
}

// if you search on ocado or tesco it changes the url without a reload
var gLastURL;
function check_for_searches() {
    var url=document.URL;
    if(url===gLastURL)
        return;

    gLastURL=url;
    console.log("Search detected - will refresh formatting in 4s");
    setTimeout(get_score_tables, 4000);
}


console.log("Ethical Shopping Helper Extension active - woot!");

// we run our code periodically to check if the go_away timeout has expired, or if the player has subscribed to EC in the meantime, or data has changed
setTimeout(get_score_tables, 2000); // Delay initial run for client side code to hopefully finish
setInterval(get_score_tables, 30000);

// tesco and ocado change the url when you search, but don't reload the entire page - so catch this and trigger a recolouring
// window.addEventListener('hashchange',..) wasn't working so let's just brute force check the url every second
gLastURL=document.URL;
setInterval(check_for_searches, 1000);



