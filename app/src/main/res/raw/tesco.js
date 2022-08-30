


function colour_page(response) {
    // grab all the tables for all the product types and munge them into a big useful struct
    munged_tables=get_munged_tables(response);

    // find the products
    // search results
    // and shopping basket big view
    document.querySelectorAll("[data-auto='product-tile--title']").forEach( function( product_div ){
        if(!product_div.parentNode.querySelector('#es-moar-infos')) {
            // want to colour in the tile-content box
            var tile_content=product_div.parentNode;
            while(tile_content && tile_content.classList && !tile_content.classList.contains("tile-content") && !tile_content.classList.contains("product-details--wrapper")) {
                tile_content=tile_content.parentNode;
            }
            if(!tile_content || !tile_content.classList) {
                console.log("Error: failed to find tile_content node for "+product_div.textContent.trim());
                tile_content=product_div.parentNode;
            }
            colour_product(munged_tables, product_div, tile_content, "es-tesco-search-result", true, product_div.textContent.trim());
        }
    });

    //viewing single product
    document.querySelectorAll('.product-details-tile__title').forEach( function( product_div ){
        if(!product_div.parentNode.querySelector('#es-moar-infos')) {
            colour_product(munged_tables, product_div, product_div.parentNode.parentNode, "es-tesco-single-product", false, product_div.textContent.trim());
        }
    });

    //little trolley at the side
    document.querySelectorAll("#mini-trolley").forEach( function( mini_trolley_div ){
        console.log("doing trolley");
        mini_trolley_div.querySelectorAll('h4').forEach( function( product_div ){
            console.log("doing trolley item");
            if(!product_div.parentNode.querySelector('#es-moar-infos')) {
                colour_product(munged_tables, product_div, product_div.parentNode.parentNode.parentNode, "es-tesco-mini-trolley", true, product_div.textContent.trim());
            }
        });
    });
}

function get_header_location() {
    return document.querySelector('.header-wrapper');
}
