/****************************************************************
 * @file viewer_three.js
 * Creating a 3D object loader using the three.js library
 *
 * Language: JavaScript
 *
 * Chrysovalantis Constantinou
 * Maria-Spyridoula Tzima
 *
 * The Cyprus Institute
 *
 * + 11/03/20 (cc): Created.
 * + 11/21/20 (cc): Code development started

****************************************************************/

/****************************************************************
  Wish list:

  1) Become able to upload zip files into clowder which will be 
  automatically unzipped. Subsequently, the files they contain will be
  accessible to the previewer. This is necessary for 3D models which come
  with a texture file.

  2) Figure out how to load GLTF files with texture files manually.
  Note that the GLTFLoader() loads everything automatically, if they
  are downloaded from sketchfab. I think the BIN file contains information
  about how the textures will be loaded.

  3) Figure out how the user will interact with the model smoothly using
  OrbitControls().

  4) Figure out how to extract the dimensions of the model and then adjust
  the dimensions so that the model displays properly in the browser. (Some models
  display really big or really small).

  5) For the moment, we are loading the three.js files from the folder js. Specifically,
  I found the files "three.min.js", "GLTFLloader.js", "OrbitControls.js" from the GitHub
  of a guy who created a website to display a single GLTF model.
  However, the three.js library is available online and we should gradually switch to loading
  the library from an online server (preferably an https server, in a fashion similar to what's
  done in "viewer_hop.js").

  6) So far we are able to load GLTF models using the GLTF Loader. I believe we can easily
  load 3D models which come in a different format using for example the FBXLoader. We need
  to extract the extension of the file and depending of the type of the file, we can use the 
  appropriate loader. This is exactly what we are doing in viewer_hop.js lines 61-70. 

  Note: You can come up with your own ideas about how to attack these issues, add your own
  items in the wish list, and whenever you make changes to the code, you can add them on the 
  top like I do. Example: adding something + 01/24/21 (cc) Added smthg. Removing smthg
  - 01/24/21 (cc) Removed smthg. (Your initials are mt). Finally, once we have something 
  functional, we can create a pull request with clowder and contribute our code. Also, we need
  to add you into the list of contributors. :)

****************************************************************/

// The function below is a jQuery function. The exact details
// about how it works are not completely clear to me but the fact is that it works.
// Configuration is an object that looks like Configuration = {tab = "tab",
// previewer = "..."}. We access Configuration using the dot and then the name of the variable

(function ($, Configuration) {

    let useTab = Configuration.tab;
    let referenceUrl = Configuration.url;
    let previewer = Configuration.previewer;

    let fileName = $('#file-name-title').text().trim();
    let fileNameExtension = fileName.substr(fileName.length - 3);
    let fileType;

    // This is a trick I use to make the console.log function to work

    console.log = console.log || function(message) { alert(message);};

    console.warn = console.warn || function(message) { alert(message);};

    console.error = console.error || function(message) { alert(message);};

    // scripts is an array containing three library files which are found under
    // the js folder. We use various functions from those library files
    // to load our model.

    // These libraries can be found online and eventually we should load
    // them from there

    let scripts =["three.min.new.js", "inflate.min.js",
        "GLTFLloader.new.js", "OrbitControls.new.js", "FBXLoader.new.js"];

    // We use the for loop to load the files into our program

    for (index = 0; index < scripts.length; index++) {
        var s = document.createElement("script");
        s.type = "text/javascript";
        s.src = previewer + "/js/" + scripts[index];
        $(useTab).append(s);
       }

    // $(document.ready()) is a jQuery function inside which you can call other functions
    // to run your program

    $(document).ready(function(){
        init();
    });


}(jQuery, Configuration));

// Here is the gist of the work. init() is the function we use to load the 3D
// model. We declare all those variables outside init(), so that they are now global
// variables (i.e., they can be used anywhere in this file. Variables declared inside
// a function are instead called local variables and they can be used inside the function.

// Every three.js program needs a camera that sits in frond of the model and shows it on
// the screen, much like what we do in the real world. The scene is the scene where we put the
// model the background colors, the camera, etc. And the renderer puts all of these things together.

// I wish I could explain what a mesh is. The loader is what loads the model.

let camera, scene, renderer;
let mesh;
let loader;

// These are variables I use to define the dimensions of the window. The container
// is the small window that shows up on the screen. You can change the numbers and see what will happen.

let container;
let previewerWidth = 640;
let previewerHeight = 480;


function init() {
    // Here is where I tell the program to create a window inside clowder and
    // show everything within that window.

    container = document.getElementById(Configuration.tab.replace("#",""));

    // Here we create a scene

    scene = new THREE.Scene();
    scene.background = new THREE.Color(0xdddddd);

    // Here is where I create a camera. And I really need your help to understand how things work.
    // This site is good but I didn't study it thoroughly:
    // https://threejsfundamentals.org/threejs/lessons/threejs-fundamentals.html
    // You can check this as well:
    // https://tympanus.net/codrops/2016/04/26/the-aviator-animating-basic-3d-scene-threejs/

    camera = new THREE.PerspectiveCamera( 70, previewerWidth/previewerHeight, 1, 1000 );
    camera.position.z = 40;

    // Here is where we load the texture.
    
    // Here is where we import the 3D model.

    // For now we save it in the clowder installation,
    // inside the folder public/javascripts/previewers/three_js/models

    //let loader = new THREE.GLTFLoader();
    //let filepath = "/assets/javascripts/previewers/three_js/models/airplane/scene.gltf";
    let loader = new THREE.FBXLoader();
    let filepath = "/assets/javascripts/previewers/three_js/models/free-1972-datsun-240k-gt/source/datsun240k.fbx";

    // This is the loader function. I don't understand all the details

    loader.load(filepath, function (obj) {

        // For the GLTF loader you need to add obj.scene to the scene

        //scene.add(obj.scene);

        // For the FBX loader you need to add the obj itself.

        scene.add(obj);

        let light = new THREE.AmbientLight( 0xffffff );
        scene.add( light );

        animate();
    });

    // Here we initialize the renderer which puts everything together as my understanding is for now.

    renderer = new THREE.WebGLRenderer( { antialias: true } );
    renderer.setPixelRatio( window.devicePixelRatio );
    renderer.setSize( previewerWidth, previewerHeight );
    container.appendChild( renderer.domElement );

    let controls = new THREE.OrbitControls(camera, renderer.domElement);
    container.appendChild(renderer.domElement);
}

// The functions below where found elsewhere and honestly I don't understand how they work.

function onWindowResize() {

    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();

    renderer.setSize( window.innerWidth, window.innerHeight );
}

function animate() {

    requestAnimationFrame( animate );
    renderer.render( scene, camera );
}

