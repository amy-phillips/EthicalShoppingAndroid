


function colour_page(response) {
    // grab all the tables for all the product types and munge them into a big useful struct
    munged_tables=get_munged_tables(response);

    // search results
    //viewing single product
    document.querySelectorAll('.name').forEach( function( product_div ){
        if(!product_div.parentNode.querySelector('#es-moar-infos')) {
            // want to colour in the tile-content box
            var tile_content=product_div.parentNode;
            while(tile_content && tile_content.classList && !tile_content.classList.contains("product-main-info") && !tile_content.classList.contains("producttile__content")) {
                tile_content=tile_content.parentNode;
            }
            if(!tile_content || !tile_content.classList) {
                console.log("Error: failed to find product-main-info/producttile__content node for "+product_div.textContent.trim());
                tile_content=product_div.parentNode;
            }
            colour_product(munged_tables, product_div, tile_content, "es-mam-single-product", true, product_div.textContent.trim());
        }
    });

    //trolley
    document.querySelectorAll('.test_item__name').forEach( function( product_div ){
        if(!product_div.parentNode.querySelector('#es-moar-infos')) {
            // want to colour in the tile-content box
            var tile_content=product_div.parentNode;
            while(tile_content && tile_content.classList && !tile_content.classList.contains("test_item__info")) {
                tile_content=tile_content.parentNode;
            }
            if(!tile_content || !tile_content.classList) {
                console.log("Error: failed to find test_item__info node for "+product_div.textContent.trim());
                tile_content=product_div.parentNode;
            }
            colour_product(munged_tables, product_div, tile_content, "es-mam-trolley", true, product_div.textContent.trim());
        }
    });

    //little trolley at the side
    document.querySelectorAll('.day-cart-name').forEach( function( product_div ){
        if(!product_div.parentNode.querySelector('#es-moar-infos')) {
            colour_product(munged_tables, product_div, product_div.parentNode.parentNode, "es-mam-mini-trolley", true, product_div.textContent.trim());
        }
    });
}

function get_header_location() {
    return document.querySelector('.container');
}
