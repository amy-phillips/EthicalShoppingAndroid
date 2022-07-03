



function colour_page(response) {
    // grab all the tables for all the product types and munge them into a big useful struct
    munged_tables=get_munged_tables(response);

    // find the products
    // search results
    document.querySelectorAll('.productNameAndPromotions h3').forEach( function( product_div ){
        // has it already been coloured?
        if(!product_div.parentNode.querySelector('#es-moar-infos')) {
            // find the product div
            var tile_content=product_div.parentNode;
            while(tile_content && tile_content.classList && !tile_content.classList.contains("productInfo")) {
                tile_content=tile_content.parentNode;
            }
            if(!tile_content || !tile_content.classList) {
                console.log("Error: failed to find productInfo node for "+product_div.textContent.trim());
                tile_content=product_div.parentNode;
            }
            colour_product(munged_tables, product_div, tile_content, "es-sainsbury-search-result", true, product_div.querySelector("a").textContent.trim());
        }
    });

    // favourites new layout with hidden trolley
    document.querySelectorAll('.pt__info__description').forEach( function( product_div ){
        // has it already been coloured?
        if(!product_div.parentNode.querySelector('#es-moar-infos')) {
            // find the product div
            var tile_content=product_div.parentNode;
            while(tile_content && tile_content.classList && !tile_content.classList.contains("pt__info")) {
                tile_content=tile_content.parentNode;
            }
            if(!tile_content || !tile_content.classList) {
                console.log("Error: failed to find pt__info node for "+product_div.textContent.trim());
                tile_content=product_div.parentNode;
            }
            colour_product(munged_tables, product_div, tile_content, "es-sainsbury-search-result", true, product_div.querySelector("a").textContent.trim());
        }
    });


    // "before you go" (as checking out)
    document.querySelectorAll('.productNameAndPromotions').forEach( function( product_div ){
        // has it already been coloured?
        if(!product_div.parentNode.querySelector('#es-moar-infos')) {
            // find the product div
            var tile_content=product_div.parentNode;
            while(tile_content && tile_content.classList && !tile_content.classList.contains("productESpot")) {
                tile_content=tile_content.parentNode;
            }
            if(!tile_content || !tile_content.classList) {
                console.log("Failed to find tile_content node for "+product_div.textContent.trim()+" probably not a before you go - not colouring");
                return;
            }
            colour_product(munged_tables, product_div, tile_content, "es-sainsbury-search-result", true, product_div.textContent.trim());
        }
    });

    //viewing single product
    document.querySelectorAll('.pd__wrapper h1').forEach( function( product_div ){
        if(!product_div.parentNode.querySelector('#es-moar-infos')) {
            var tile_content=product_div.parentNode;
            while(tile_content && tile_content.classList && !tile_content.classList.contains("productSummary")) {
                tile_content=tile_content.parentNode;
            }
            if(!tile_content || !tile_content.classList) {
                console.log("Error: failed to find tile_content node for "+product_div.textContent.trim());
                tile_content=product_div.parentNode;
            }
            colour_product(munged_tables, product_div, tile_content, "es-sainsbury-single-product", false, product_div.textContent.trim());
        }
    });

    //shopping basket
    document.querySelectorAll('.productContainer > a').forEach( function( product_div ){
        if(!product_div.parentNode.querySelector('#es-moar-infos')) {
            colour_product(munged_tables, product_div, product_div.parentNode, "es-sainsbury-shopping-basket", true, product_div.textContent.trim());
        }
    });
    // favourites pop up shopping basket
    document.querySelectorAll('.pt-mini__title').forEach( function( product_div ){
        if(!product_div.parentNode.querySelector('#es-moar-infos')) {
            colour_product(munged_tables, product_div, product_div.parentNode, "es-sainsbury-shopping-basket", true, product_div.textContent.trim());
        }
    });

    //little trolley at the side
    document.querySelectorAll('.trolley-summary__list-container').forEach( function( trolley_div ){
        console.log("doing trolley");
        // find all the products
        trolley_div.querySelectorAll('.trolley-item__product').forEach( function( product_div ){
            console.log("doing trolley product");
            // and finally find the a linky doodah
            var linky_seggy=product_div.querySelector('a');
            if(!linky_seggy.parentNode.querySelector('#es-moar-infos')) {
                colour_product(munged_tables, linky_seggy, linky_seggy.parentNode, "es-sainsbury-mini-trolley", true, linky_seggy.textContent.trim());
            }
        });
    });
}

function get_header_location() {
    return document.querySelector('#globalHeaderContainer') || document.querySelector('.ln-o-page__header');
}
