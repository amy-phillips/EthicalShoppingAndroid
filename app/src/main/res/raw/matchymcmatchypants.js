
//mmmm copy pasta from https://stackoverflow.com/questions/27194359/javascript-pluralize-a-string
String.prototype.plural = function(revert){

    var plural = {
        '(quiz)$'               : "$1zes",
        '^(ox)$'                : "$1en",
        '([m|l])ouse$'          : "$1ice",
        '(matr|vert|ind)ix|ex$' : "$1ices",
        '(x|ch|ss|sh)$'         : "$1es",
        '([^aeiouy]|qu)y$'      : "$1ies",
        '(hive)$'               : "$1s",
        '(?:([^f])fe|([lr])f)$' : "$1$2ves",
        '(shea|lea|loa|thie)f$' : "$1ves",
        'sis$'                  : "ses",
        '([ti])um$'             : "$1a",
        '(tomat|potat|ech|her|vet)o$': "$1oes",
        '(bu)s$'                : "$1ses",
        '(alias)$'              : "$1es",
        '(octop)us$'            : "$1i",
        '(ax|test)is$'          : "$1es",
        '(us)$'                 : "$1es",
        '([^s]+)$'              : "$1s"
    };

    var singular = {
        '(quiz)zes$'             : "$1",
        '(matr)ices$'            : "$1ix",
        '(vert|ind)ices$'        : "$1ex",
        '^(ox)en$'               : "$1",
        '(alias)es$'             : "$1",
        '(octop|vir)i$'          : "$1us",
        '(cris|ax|test)es$'      : "$1is",
        '(shoe)s$'               : "$1",
        '(o)es$'                 : "$1",
        '(bus)es$'               : "$1",
        '([m|l])ice$'            : "$1ouse",
        '(x|ch|ss|sh)es$'        : "$1",
        '(m)ovies$'              : "$1ovie",
        '(s)eries$'              : "$1eries",
        '([^aeiouy]|qu)ies$'     : "$1y",
        '([lr])ves$'             : "$1f",
        '(tive)s$'               : "$1",
        '(hive)s$'               : "$1",
        '(li|wi|kni)ves$'        : "$1fe",
        '(shea|loa|lea|thie)ves$': "$1f",
        '(^analy)ses$'           : "$1sis",
        '((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$': "$1$2sis",        
        '([ti])a$'               : "$1um",
        '(n)ews$'                : "$1ews",
        '(h|bl)ouses$'           : "$1ouse",
        '(corpse)s$'             : "$1",
        '(us)es$'                : "$1",
        's$'                     : ""
    };

    var irregular = {
        'move'   : 'moves',
        'foot'   : 'feet',
        'goose'  : 'geese',
        'sex'    : 'sexes',
        'child'  : 'children',
        'man'    : 'men',
        'tooth'  : 'teeth',
        'person' : 'people',
        'bean'   : 'beanz', // for heinz
        'spread' : 'spreadable', //yes this is abusing the concept of plurality - sorry not sorry!
        'cereal' : 'muesli',
        'bread'  : 'loaf',
    };

    var uncountable = [
        'sheep', 
        'fish',
        'deer',
        'moose',
        'series',
        'species',
        'money',
        'rice',
        'information',
        'equipment'
    ];

    // save some time in the case that singular and plural are the same
    if(uncountable.indexOf(this.toLowerCase()) >= 0)
      return String(this);

    // check for irregular forms
    for(word in irregular){

      if(revert){
              var pattern = new RegExp(irregular[word]+'$', 'i');
              var replace = word;
      } else{ var pattern = new RegExp(word+'$', 'i');
              var replace = irregular[word];
      }
      if(pattern.test(this))
        return this.replace(pattern, replace);
    }

    if(revert) var array = singular;
         else  var array = plural;

    // check for matches using regular expressions
    for(reg in array){

      var pattern = new RegExp(reg, 'i');

      if(pattern.test(this))
        return this.replace(pattern, array[reg]);
    }

    return String(this);
}

// matches [a], [o], [a,o] etc
var ETHICAL_CONSUMER_MARKUP = /\[[afgorsv\s]+(?:,[afgorsv\s]+)*,?\]/gmi;
var PUNCTUATION = /[^\w\s]/gmi;

// takes the title of an ethical consumer section and processes it into keywords you'd expect to find in a food title - eg coffee
function pre_process_food(name) {
 
    processed=name.replace('&',' ');
    processed=processed.replace(/\-/gi,' '); //washing-up should become washing up, rather than washingup
    processed=processed.replace(/spreads/gi,'butter');

    var plurals = processed.toLowerCase().split(/\s+/);
    var singles=new Set(); // no dups plz
    for(let word of plurals) {
        var single=word.plural(true);
        singles.add(single);
    }

    // title words that might not be in the product name on sainsburys so no point keeping them?
    singles.delete('ethical');
    singles.delete('vegan');
    singles.delete('dairy');
    singles.delete('free');
    singles.delete('and');
    singles.delete('cooking');
    singles.delete(''); // if we split a whitespace string we get a single empty entry - doh!

    //console.log(name + " ("+processed+") becomes:");
    //console.log(Array.from(singles).join(', '));
    // we want at least one of these words to match in food description, but don't require them all
    return {"name":name,"all_of":[],"one_of":Array.from(singles)};
}

function pre_process(name,pre_processed_food=null) {
    // remove any [a] or [o] etc from ethical consumer
    processed=name.replace(ETHICAL_CONSUMER_MARKUP,'');
    // remove accented characters https://stackoverflow.com/questions/990904/remove-accents-diacritics-in-a-string-in-javascript
    processed=processed.normalize('NFD');
    //use a regex rather than a string as the replace arg because we want to replace all instances, not just the first
    processed=processed.replace(/\-/gi,' '); //washing-up should become washing up, rather than washingup
    processed=processed.replace(PUNCTUATION,'');
    processed=processed.replace(/tinned/gi,'');
    processed=processed.replace(/MSC/gi,'');
    //ethical consumer is all american with their laundry detergent
    processed=processed.replace(/washing (powder|gel|liquid|capsules)/gi,'laundry detergent');
    processed=processed.replace(/laundry liquid/gi,'laundry detergent');
    processed=processed.replace(/washing powder and liquid/gi,'laundry detergent');
    processed=processed.replace(/dishwasher (powder|tablets|tabs)/gi,'dishwasher detergent');
    // the type of squash doesn't match for Rocks so remove the flavour
    processed=processed.replace(/(blackcurrant|orange) squash/gi,'squash');
    // the flavour of bio-d washing up liquid doesn't matter either
    //processed=processed.replace(/(lavender) laundry detergent/gi,'laundry detergent');
    processed=processed.replace(/jaffa cake/gi,'biscuit');
    // milk&more doesn't list kingsmill bread as bread, so add in the keyword bread to make it match
    processed=processed.replace(/allinsons .* (white|wholemeal)/gi,'allinsons bread');
    processed=processed.replace(/kingsmill (super seeds|5050|wholemeal|tasty wholemeal|soft white)/gi,'kingsmill bread');
    // don't care if Kelly's ice cream is in tubs!
    processed=processed.replace(/tubs/gi,'');
    processed=processed.replace(/of cornwall/gi,'cornish');
    // Haagen Dazs ice cream outside USA
    processed=processed.replace(/outside USA/gi,'');
    // Wall's ice cream & lollies - let's just pretend lollies are ice cream
    processed=processed.replace(/lollies/gi,'ice cream');
    // Good earth teabags - let's say teabags are just tea
    processed=processed.replace(/teabags/gi,'tea');
    // the butter and spreads section is annoying - pretend they're all butter
    processed=processed.replace(/spread/gi,'butter');
    processed=processed.trim();
   
    // split into words, make all lowercase, and not plural
    var plurals = processed.toLowerCase().split(/\s+/);
    var singles=new Set(); // no dups plz
    for(let word of plurals) {
        var single=word.plural(true);
        singles.add(single);
    }

    // a bunch of coffee is listed like ground, beans & instant, or beans & ground, or ground & beans,
    // but we don't want to contaminate baked beans
    coffee_like=0;
    coffee_words = new Set(['ground', 'bean', 'instant', 'coffee'])
    for(let word of singles) {
        if(coffee_words.has(word)) {
            coffee_like++;
        }
    }
    if(coffee_like>=2) { // joyful magical number threshold gogogogo :)
        // it's all just coffee innit?
        singles.delete('ground');
        singles.delete('bean');
        singles.delete('instant');
        singles.add('coffee');
    }

    // if this was in a well-named section on ethical consumer, add that too
    one_of = new Set();
    if (pre_processed_food) {
        pre_processed_food.all_of.forEach(singles.add, singles);
        pre_processed_food.one_of.forEach(one_of.add, one_of);

        // if any of the one_ofs are in the all_ofs, then remove them from the one_ofs, they are not adding any info!
        singles.forEach(one_of.delete, one_of);
    }

    //console.log(name + " ("+processed+") becomes:");
    //console.log(Array.from(singles).join(', '));
    //console.log(Array.from(one_of).join(', '));
    // if this was in a well-named section on ethical consumer, add that too
    return {"name":name,"one_of":Array.from(one_of),"all_of":Array.from(singles)};
}

// get a number between 0 (not at all matchy) and 1 (really quite matchy)
function get_matchiness(product_name, ec_name) {
    matching_word_count=0;
    for(let word1 of product_name.all_of) {
        for(let word2 of ec_name.all_of) {
            // do the words match, either exactly or with a trailing s?
            if(word1==word2) {
                matching_word_count++;
                break;
            }

            // todo similarity??
        }

        for(let word2 of ec_name.one_of) {
            // do the words match, either exactly or with a trailing s?
            if(word1==word2) {
                matching_word_count++;
                break;
            }

            // todo similarity??
        }
    }
  
    return matching_word_count/Math.min(product_name.all_of.length,(ec_name.all_of.length+ec_name.one_of.length));
}
